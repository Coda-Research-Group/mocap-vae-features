import math
import time
from tslearn.metrics import dtw_path_from_metric, dtw
import torch
from torch.nn import functional as F
import numpy as np
from sklearn.neighbors import NearestNeighbors
from random import random
from sklearn.metrics import recall_score

path = "/home/drking/Documents/Bakalarka/data/class130-actions-segment80_shift16-coords_normPOS-fps12.data"

class MocapObject:
    def __init__(self, obj_id, data, num_frames):
        self.obj_id = obj_id
        self.data = torch.tensor(data, requires_grad=False)
        self.data = self.data / self.data.norm(dim=1)[:, None]
        self.num_frames = num_frames 
        self.class_id = int(obj_id.split("_")[1])

# def utw_distance(obj1, obj2):
#     matrix = torch_cosine_similarity(obj1, obj2)

#     a = matrix.shape[0]
#     b= matrix.shape[1]
#     m = max(matrix.shape)
#     t_ind_a = torch.round(torch.linspace(0,a-1, steps=m)).int()
#     t_ind_b = torch.round(torch.linspace(0,b-1, steps=m)).int()

#     return matrix[t_ind_a,t_ind_b].sum()


def utw_distance(obj1:MocapObject, obj2:MocapObject):
    m = max(obj1.num_frames,obj2.num_frames)
    if obj2.num_frames > obj1.num_frames:
        t_ind_a = torch.round(torch.linspace(0,obj1.num_frames-1, steps=m)).int()
        t_ind_b = np.arange(obj2.num_frames)
    elif obj1.num_frames > obj2.num_frames:
        t_ind_a = np.arange(obj1.num_frames)
        t_ind_b = torch.round(torch.linspace(0,obj2.num_frames-1, steps=m)).int()
    else:
        t_ind_a = np.arange(obj1.num_frames)
        t_ind_b = np.arange(obj2.num_frames)
    return torch_cosine_similarity(obj1, obj2)[t_ind_a,t_ind_b].sum()


           
    
# Cosine similarity between two sequences calculated with torch
def cosine_similarity_between_sequences_raw(obj1 : MocapObject, obj2 : MocapObject):
    # Convert to torch tensors
    dist_mat = torch.zeros((obj1.num_frames, obj2.num_frames))
    for i in range(obj1.num_frames):
        for j in range(0,obj2.num_frames):
            dist = F.cosine_similarity(obj1.data[i], obj2.data[j], dim=0)
            dist_mat[i][j] = dist
    dist_mat = 1 - dist_mat
    # dist_mat = dist_mat.sum(0).numpy()

    return dist_mat

def torch_cosine_similarity(obj1, obj2):
    a = obj1.data
    b = obj2.data
    return 1 - torch.mm(a, b.transpose(0,1))

def distance_between_sequences_raw(obj1:MocapObject, obj2: MocapObject):
    matrix = torch_cosine_similarity(obj1, obj2)

    # matrix = cosine_similarity_between_sequences_raw(obj1, obj2)
    # cost = random()
    path, cost = dtw_path_from_metric(matrix.numpy(), metric="precomputed")
    if cost < 0:
        return 0
    return cost

# database = list()
# with open(path) as file:
#     current_obj = MocapObject("", [], 0)
#     for line in file:
#         line = line.rstrip()
#         if (line.startswith("#")):
#             current_obj = MocapObject("", [], 0)
#             current_obj.obj_id = line.split(" ")[2]
#             current_obj.class_id = int(current_obj.obj_id.split("_")[1])
#             database.append(current_obj)
#         elif ("mcdr" in line):
#             current_obj.num_frames = int(line.split(";")[0])
#         else :
#             splits = line.split(",")
#             float_data = [float(x) for x in splits]
#             current_obj.data.append(float_data)
database = list()
with open(path) as file:
    obj_id = ""
    class_id = -1
    vectors = []
    num_frames = 0
    for line in file.readlines():
        line = line.rstrip()
        if (line.startswith("#")):
            if num_frames > 0:
                database.append(MocapObject(obj_id, vectors, num_frames))
                vectors = []
            obj_id = line.split(" ")[2]
        elif ("mcdr" in line):
            num_frames = int(line.split(";")[0])
        else :
            splits = line.split(",")
            float_data = [float(x) for x in splits]
            vectors.append(float_data)
print(f"Loaded {len(database)} objects")


#distance matrix
start = time.time()
distance_matrix_data = np.ndarray((len(database), len(database)))

for i in range(len(database)):
    # if (i % 10 == 0):
    print(f"Processed {i} objects")
    for j in range(i, len(database)):
        if (i == j):
            distance = 500000
        else:
            distance = utw_distance(database[i], database[j])
            # distance = distance_between_sequences_raw(database[i], database[j])
        distance_matrix_data[i][j] = distance
        distance_matrix_data[j][i] = distance
end = time.time()
print("elapsed time for matrix " + str(end - start))

#1nn from dist matrix
nn = NearestNeighbors(n_neighbors=1, metric="precomputed")
nn.fit(distance_matrix_data)
distances, indices = nn.kneighbors(distance_matrix_data)


predicted = []
gt = []
for i in range(len(indices)):
    # print(f"Object {database[i].obj_id} is closest to object {database[indices[i][0]].obj_id} with distance {distances[i][0]}")
    predicted.append(database[indices[i][0]].class_id)
    gt.append(database[i].class_id)

# compute recall 
recall = recall_score(gt, predicted, average='micro')
print(f"Recall: {recall}")
