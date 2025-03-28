# Clustering

## Skeleton Division

- IDs correspond to the IDs in `Joint.java`
- index != ID, ID = index + 1
- 5 body parts (+ 3 relations)
- HDM05
  - body parts: (`2,3,4,5,6`) (`7,8,9,10,11`) (`12,13,14,15,16,17`) (`18,19,20,21,22,23,24`) (`25,26,27,28,29,30,31`)
  - relations: (`28,29,30,31,21,22,23,24`) (`16,17,28,29,30,31`) (`16,17,21,22,23,24`)
    - consisting of: RH (`28,29,30,31`), LH (`21,22,23,24`), and HEAD (`16,17`)
- PKU-MMD
  - body parts: (`2,3,4,21`) (`5,6,7,8,22,23`) (`9,10,11,12,24,25`) (`13,14,15,16`) (`17,18,19,20`)
  - relations: (`11,12,24,25,7,8,22,23`) (`3,4,11,12,24,25`) (`3,4,7,8,22,23`)
    - consisting of: RH (`11,12,24,25`), LH (`7,8,22,23`), and HEAD (`3,4`)

## Outline of scripts in `./scripts`

**Data splitting**

- `create-n-fold-cross-validation-ids.py` - splits the dataset n times (~split), each split is divided into m folds, labels of each of actions in each split in each fold are stored collectively
- `create-n-fold-cross-validation-data.py` - based on the stored labels, filters the actual data and stores them
- `create-n-fold-cross-validation-clustering.py` - based on the stored data, creates files ready for the clustering process

**Clustering and conversion into Composite MWs**

- `cluster.sh` - clustering, selection of medoids
  - `createCompositeMWClusteringELKI` - clustering using ELKI (<http://elki.dbs.ifi.lmu.de/releases/release0.7.5/doc/de/lmu/ifi/dbs/elki/algorithm/clustering/kmeans/KMedoidsFastPAM.html>)
    1. `createClusters` - Runs clustering on ELKI formatted dataset, ELKI clustering folder is produced.
    2. `convertElkiClusteringFormatToElkiFormat` - Converts the result clusters from ELKI clustering format to the ELKI format. Uses `Convertor` class (`ElkiConversion`).
    3. `runKMedoidsClusteringOnEveryCluster` - Runs k-medoids (`KMedoidsFastPAM`) clustering on every converted cluster with `k=1`.
    4. `extractClusterMedoids` - Extracts a medoid from every cluster and convert it to MESSIF format. Uses `Convertor` class (`MedoidParsing`). The number of medoids is equal to number of clusters created in `1.1`.
  - `createCompositeMWClusteringMessif` - clustering using MESSIF (<https://gitlab.fi.muni.cz/disa/public/messif-utils/-/blob/master/src/main/java/SelectPivots.java>)
- `convert-to-mws.sh` - conversion of data into MWs (each body part into Hard MW)
- `combine-into-composite-mw.sh` - combines Hard MWs into Composite MW using `CompositeMWCombiner.java`

**Other**

- `merge-categories.py` - merges categories of HDM05-130 dataset into HDM05-65, implemented by substituting labels in the data file
- `convert-from-messif.pl` - MESSIF to ELKI format conversion, see `Convertor.java`

## Runnable Java classes

In `src/main/java/clustering`:

- `ELKIWithDistances` provides ELKI CLI and GUI access to the distance functions located in `./src/main/java/clustering/distance` folder.
- `Convertor` converts between various formats of ELKI and MESSIF.
- `CompositeMWCombiner` combines multiple Hard MW files into a single Composite MW file.

## Notes

- As the dataset (`elki-class130-actions-segment80_shift16-coords_normPOS-fps12.data`) is normalized the first joint (ROOT, ID 1, střed pánve) has always 3D coordinates equal to (0,0,0).

## Conversion of actions from MESSIF dataset format into ELKI format

```shell
# Usage
perl convert-from-messif.pl <input_dataset_path> > <output_dataset_path>

# Example
perl convert-from-messif.pl actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data > elki-actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data
```

## Project Terminology (relevant to the ELKI clustering)

**MESSIF format** - the format of the MESSIF dataset, e.g `class130-actions-segment80_shift16-coords_normPOS-fps12.data`

```text
#objectKey messif.objects.keys.AbstractObjectKey 3136_100_1245_236_0
8;mcdr.objects.ObjectMocapPose
0.0, 0.0, 0.0; 1.3196042, -1.9361438, 0.9896419; ...
...
...
```

**ELKI format** - the format of the data transformed for the purpose of ELKI clustering, transformed from the original dataset in **MESSIF format**, e.g. `elki-class130-actions-segment80_shift16-coords_normPOS-fps12.data`

```text
8 0.0, 0.0, 0.0, 1.3196042, ..., -2.564895, 0.97724426 label3136_100_1245_236_0
...
...
```

**ELKI clustering folder** - the folder containing results (e.g. `cluster-evaluation.txt`, `settings.txt`, `cluster_0.txt`, ...) of the ELKI clustering

**ELKI clustering file** - the file containing cluster information inside **ELKI clustering folder**, e.g. `cluster_3.txt`

**ELKI clustering format** - the format of the **ELKI clustering file**

```text
# Cluster: Cluster 0
# Cluster name: Cluster
# Cluster noise flag: false
# Cluster size: 131
# Model class: de.lmu.ifi.dbs.elki.data.model.MedoidModel
# Cluster Medoid: 8038
ID=339 8.0 0.0 0.0 0.0 1.3196042 ... -2.564895 0.97724426 label3136_100_1245_236_0
...
...
```

**ELKI clustering object** - the object created as result of ELKI clustering, a line containing an object in the **ELKI clustering file**

```text
ID=339 8.0 0.0 0.0 0.0 1.3196042 ... -2.564895 0.97724426 label3136_100_1245_236_0
```
