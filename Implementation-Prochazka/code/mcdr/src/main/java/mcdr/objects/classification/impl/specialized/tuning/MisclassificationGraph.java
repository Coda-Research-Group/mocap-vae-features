package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.classification.impl.ClassificationResult;
import mcdr.test.utils.ObjectCategoryMgmt.Category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A graph representation of the misclassification tendencies extracted from the global classification result.
 *
 * @author David Procházka
 */
final class MisclassificationGraph {

    /**
     * A representation of the misclassification tendencies.
     * The {@code Integer} number of objects is misclassified from the outer {@code Category}
     * into the inner {@code Category}.
     */
    private final Map<Category, Map<Category, Integer>> tendencies = new HashMap<>();

    /**
     * Creates a graph representation of the misclassification tendencies based on the global classification result.
     *
     * @param globalClassificationResult the global classification result
     * @return the graph representation of the misclassification tendencies
     */
    static MisclassificationGraph create(ClassificationResult globalClassificationResult) {
        var graph = new MisclassificationGraph();

        var classificationResultPerCategory = globalClassificationResult.classificationResultPerCategory();

        for (var result : classificationResultPerCategory.entrySet()) {
            var category = result.getKey();
            var classifiedActions = result.getValue();

            for (var action : classifiedActions) {
                if (action.isMisclassified()) {
                    graph.increaseTendencyBetween(category, action.classifiedCategory(), 1);
                }
            }
        }

        return graph;
    }


    /**
     * Creates a new transposed graph. Does not affect the existing one.
     *
     * @param graph the graph to transpose
     * @return the new transposed graph
     */
    private static MisclassificationGraph transpose(MisclassificationGraph graph) {
        var transposed = new MisclassificationGraph();

        graph.tendencies.forEach((fromCategory, toCategories) -> {
            toCategories.forEach((toCategory, count) -> {
                transposed.increaseTendencyBetween(toCategory, fromCategory, count);
            });
        });

        return transposed;
    }

    /**
     * Filters the classification categories based on threshold limiting their maximum count.
     * Corresponds to the first min(t, |N_v|) classification categories in descending order.
     *
     * @param classificationCategories                         the classification categories with their counts - N_v
     * @param maxClassificationCategoriesPerInvocationCategory the threshold - t
     * @return the filtered classification categories
     */
    private static Set<Category> filter(Map<Category, Integer> classificationCategories, long maxClassificationCategoriesPerInvocationCategory) {
        return classificationCategories
                .entrySet()
                .stream()
                .sorted(Comparator.<Entry<Category, Integer>>comparingInt(Entry::getValue).reversed())
                .limit(maxClassificationCategoriesPerInvocationCategory)
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }


    /**
     * Creates category associations based on misclassification tendencies.
     * The given parameter limits the number of classification categories per invocation category.
     *
     * @param maxClassificationCategoriesPerInvocationCategory the threshold - t
     * @return the category associations
     */
    List<CategoryAssociation> createCategoryAssociations(long maxClassificationCategoriesPerInvocationCategory) {
        var associations = new ArrayList<CategoryAssociation>();

        var transposedGraph = transpose(this);

        for (var tendency : transposedGraph.tendencies.entrySet()) {
            var invocationCategory = tendency.getKey();
            var classificationCategories = tendency.getValue();

            // Adds self-loops to also distinguish between invocation category actions.
            classificationCategories.put(invocationCategory, Integer.MAX_VALUE);

            var filteredClassificationCategories = filter(classificationCategories, maxClassificationCategoriesPerInvocationCategory);

            associations.add(new CategoryAssociation(invocationCategory, filteredClassificationCategories));
        }

        return associations;
    }

    /**
     * Returns the number of invocation categories.
     *
     * @return the number of invocation categories
     */
    int size() {
        return tendencies.size();
    }

    /**
     * Adds the given count to the misclassification tendency count between the given categories.
     *
     * @param from  the category from which the misclassification originated
     * @param to    the category into which the misclassification occurred
     * @param count the count
     */
    private void increaseTendencyBetween(Category from, Category to, int count) {
        tendencies
                .computeIfAbsent(from, category -> new HashMap<>())
                .merge(to, count, Integer::sum);
    }

    @Override
    public String toString() {
        return tendencies.toString();
    }
}
