package org.aksw.autosparql.tbsl.algorithm.learning.feature;

import java.util.Map;
import java.util.Map.Entry;

import org.aksw.autosparql.commons.knowledgebase.Knowledgebase;
import org.aksw.autosparql.tbsl.algorithm.learning.Entity;
import org.aksw.autosparql.tbsl.algorithm.learning.TemplateInstantiation;
import org.aksw.autosparql.tbsl.algorithm.sparql.Slot;
import org.aksw.autosparql.tbsl.algorithm.util.Prominences;
import org.aksw.autosparql.tbsl.algorithm.util.Statistics;

public class EntityProminenceFeatureExtractor extends AbstractFeatureExtractor{

	private Map<Slot, Prominences> prominenceScores;

	public EntityProminenceFeatureExtractor(Knowledgebase knowledgebase, Map<Slot, Prominences> prominenceScores) {
		super(knowledgebase);
		this.prominenceScores = prominenceScores;
	}

	@Override
	public double extractFeature(TemplateInstantiation templateInstantiation) {
		double totalScore = 0;
		Map<Slot, Entity> allocations = templateInstantiation.getAllocations();
		for(Entry<Slot, Entity> entry : allocations.entrySet()){
			Slot slot = entry.getKey();
			Entity entity = entry.getValue();
			Prominences prominencesValues = prominenceScores.get(slot);
			double prominence = prominencesValues.get(entity);
			//normalize the score by min-max norm
			double normalizedProminence = Statistics.minMaxNorm(prominence, prominencesValues.getMax(), prominencesValues.getMin());
			totalScore += normalizedProminence;
		}
		//compute the average
//		totalScore /= templateInstantiation.getTemplate().getSlots().size();
		totalScore /= allocations.size();
		return totalScore;
	}

	@Override
	public Feature getFeature() {
		return Feature.PROMINENCE_AVERAGE;
	}

}