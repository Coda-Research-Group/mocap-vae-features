package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.distance.LevenshteinDistance;
import mcdr.distance.RepetitionDistance;
import mcdr.objects.classification.impl.ClassificationResult;
import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import mcdr.objects.classification.impl.specialized.SpecializedClassifier;
import mcdr.objects.classification.impl.specialized.SpecializedClassifierConfig;
import mcdr.objects.classification.impl.specialized.SpecializedClassifierImpl;
import mcdr.objects.impl.ObjectMotionWordComposite;
import mcdr.objects.impl.ObjectMotionWordCompositeAutoTuning;
import mcdr.test.utils.ObjectCategoryMgmt.Category;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.classification.ClassificationException;
import messif.utility.reflection.NoSuchInstantiatorException;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Encapsulates the tuning process of specialized classifiers.
 * 
 * @author David Procházka
 */
public final class Tuner {

    private static final Logger logger = Logger.getLogger(Tuner.class.getName());

    public static long MAX_CLASSIFICATION_CATEGORIES_PER_INVOCATION_CATEGORY = Long.MAX_VALUE;
    private final int k;
    private final ObjectMgmt dataMgmt;
    private final ObjectMgmt originalActionMgmt;

    public Tuner(
            int k,
            ObjectMgmt dataMgmt,
            ObjectMgmt originalActionMgmt
    ) {
        this.k = k;
        this.dataMgmt = dataMgmt;
        this.originalActionMgmt = originalActionMgmt;
    }

    private static void printClassifierStats(Map<String, SpecializedClassifier> specializedClassifiers) {
        long distinctSpecializedClassifiers = countDistinctSpecializedClassifiers(specializedClassifiers);

        List<SpecializedClassifierConfig> classifierConfigs = extractDistinctConfigurations(specializedClassifiers);
        long bodyPartClassifierCount = countBodyPartClassifiers(classifierConfigs);
        long extremaNeighborhoodClassifierCount = countExtremumNeighborhoodClassifiers(classifierConfigs);

        logger.info("Specialized classifiers: %d".formatted(distinctSpecializedClassifiers));
        logger.info("BodyPart classifiers: %d".formatted(bodyPartClassifierCount));
        logger.info("ExtremaNeighborhood classifiers: %d".formatted(extremaNeighborhoodClassifierCount));

        logger.info("Configurations:");
        for (var config : classifierConfigs) {
            logger.info(config.format());
        }
    }

    private static long countDistinctSpecializedClassifiers(Map<String, SpecializedClassifier> specializedClassifiers) {
        return specializedClassifiers
                .values()
                .stream()
                .distinct()
                .count();
    }

    private static long countExtremumNeighborhoodClassifiers(List<SpecializedClassifierConfig> classifierConfigs) {
        return classifierConfigs
                .stream()
                .filter(ExtremumNeighborhoodConfig.class::isInstance)
                .count();
    }

    private static long countBodyPartClassifiers(List<SpecializedClassifierConfig> classifierConfigs) {
        return classifierConfigs
                .stream()
                .filter(BodyPartConfig.class::isInstance)
                .count();
    }

    private static List<SpecializedClassifierConfig> extractDistinctConfigurations(Map<String, SpecializedClassifier> specializedClassifiers) {
        return specializedClassifiers
                .values()
                .stream()
                .distinct()
                .map(SpecializedClassifier::config)
                .toList();
    }

    public Map<String, SpecializedClassifier> tuneSpecializedClassifiers(ClassificationResult globalClassificationResult) throws IOException, ClassificationException, NoSuchInstantiatorException {
        long startTime = System.currentTimeMillis();

        logger.info("Running auto-tuning");

        var misclassificationGraph = MisclassificationGraph.create(globalClassificationResult);
        logger.info("Misclassification graph size: %d".formatted(misclassificationGraph.size()));
        logger.info("Misclassification graph: %s".formatted(misclassificationGraph));

        var categoryAssociations = misclassificationGraph.createCategoryAssociations(MAX_CLASSIFICATION_CATEGORIES_PER_INVOCATION_CATEGORY);
        logger.info("Invocation and classification categories: %s".formatted(categoryAssociations));

        var specializedClassifiers = new HashMap<String, SpecializedClassifier>();

        int i = 1;
        for (var association : categoryAssociations) {
            logger.info("(%d/%d) ".formatted(i, categoryAssociations.size()));
            i++;

            var classifierConfig = tuneSpecializedClassifier(association.invocationCategory(), association.classificationCategories());
            var invocationCategoryId = classifierConfig.getInvocationCategoryId();

            specializedClassifiers.put(invocationCategoryId, new SpecializedClassifierImpl(classifierConfig));
        }

        printClassifierStats(specializedClassifiers);
        logger.info("Tuning took: %ss".formatted((System.currentTimeMillis() - startTime) / 1_000.0f));

        return specializedClassifiers;
    }

    private SpecializedClassifierConfig tuneSpecializedClassifier(Category invocationCategory, Set<Category> classificationCategories) throws ClassificationException {
        var classificationCategoriesDataMgmt = dataMgmt.copy(classificationCategories);

        logger.info("Tuning: %s -> %s, %d actions".formatted(
                invocationCategory,
                Category.format(classificationCategories),
                classificationCategoriesDataMgmt.getObjectCount()
        ));

        var classifier = new ObjectMultiCategoryClassifier(true);
        var levenshteinDistance = new LevenshteinDistance<List<ObjectMotionWordCompositeAutoTuning>>();
        var repetitionDistance = new RepetitionDistance(new LevenshteinDistance<>());

        // Body Part method
        var bodyPartMethod = new BodyPartSelector(classificationCategoriesDataMgmt, classificationCategoriesDataMgmt, classifier, k);
        var primaryBodyPartResult = bodyPartMethod.selectPrimaryBodyPart(ObjectMotionWordComposite.getBodyParts(), levenshteinDistance);
        var primaryBodyPart = primaryBodyPartResult.primaryBodyPart();
        logger.info(" %s: %s".formatted(primaryBodyPart, primaryBodyPartResult.performance()));

        var bodyPartsWithoutPrimary = new HashSet<>(ObjectMotionWordComposite.getBodyParts());
        bodyPartsWithoutPrimary.remove(primaryBodyPart);
        var secondaryBodyPartResult = bodyPartMethod.selectSecondaryBodyPart(primaryBodyPart, bodyPartsWithoutPrimary, levenshteinDistance);
        var secondaryBodyPart = secondaryBodyPartResult.secondaryBodyPart();
        logger.info(" %s || %s: %s".formatted(primaryBodyPart, secondaryBodyPart, secondaryBodyPartResult.performance()));

        var primaryRepeatedBodyPartResult = bodyPartMethod.selectPrimaryBodyPart(Set.of(primaryBodyPart), repetitionDistance);
        logger.info(" Repeated %s: %s".formatted(primaryBodyPart, primaryRepeatedBodyPartResult.performance()));

        var secondaryRepeatedBodyPartResult = bodyPartMethod.selectSecondaryBodyPart(primaryBodyPart, Set.of(secondaryBodyPart), repetitionDistance);
        logger.info(" Repeated %s || %s: %s".formatted(primaryBodyPart, secondaryBodyPart, secondaryRepeatedBodyPartResult.performance()));

        var bestOfBPs = List.of(
                primaryBodyPartResult,
                secondaryBodyPartResult,
                primaryRepeatedBodyPartResult,
                secondaryRepeatedBodyPartResult
        );

        var bestPerformingBPResult = Collections.max(bestOfBPs, Comparator.comparing(BodyPartResult::performance));
        var bodyPartConfig = new BodyPartConfig(
                invocationCategory,
                classificationCategories,
                k,
                classificationCategoriesDataMgmt,
                bestPerformingBPResult
        );
        if (bestPerformingBPResult.performance() > 99.99) {
            return bodyPartConfig;
        }

        // Extremum Neighborhood method
        var classificationCategoriesOriginalActionMgmt = originalActionMgmt.copy(classificationCategories);
        var extremaNeighborhoodSelector = new ExtremumNeighborhoodSelector(classificationCategoriesDataMgmt, classificationCategoriesDataMgmt, classificationCategoriesOriginalActionMgmt, classifier, k);
        var neighborhoodResult = extremaNeighborhoodSelector.select();
        logger.info(" %s".formatted(neighborhoodResult));

        if (bestPerformingBPResult.performance() >= neighborhoodResult.performance()) {
            return bodyPartConfig;
        } else {
            return new ExtremumNeighborhoodConfig(
                    invocationCategory,
                    classificationCategories,
                    k,
                    classificationCategoriesDataMgmt,
                    neighborhoodResult,
                    originalActionMgmt
            );
        }
    }
}
