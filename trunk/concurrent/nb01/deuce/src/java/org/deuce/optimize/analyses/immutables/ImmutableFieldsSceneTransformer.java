package org.deuce.optimize.analyses.immutables;

import java.util.List;
import java.util.Map;

import org.deuce.optimize.analyses.leakyconstructor.NewlyAllocatedInCtorTagger;
import org.deuce.optimize.utils.Logger;

import soot.SceneTransformer;
import soot.SootField;
import soot.Transformer;

public class ImmutableFieldsSceneTransformer extends SceneTransformer {

	private static Transformer instance = new ImmutableFieldsSceneTransformer();
	private FieldsMutabilityDatabase database;

	@Override
	protected void internalTransform(String phaseName, Map options) {

		// what is an immutable field?
		// it is a field whose value appears to be unchanged to any thread that reads it.
		// such fields need not be logged or validated in transactions, since their values
		// are always the same.

		// this is how the algorithm works. first, find the set of all fields of all classes. then,
		// start pruning:
		// 1. prune all fields that are written to by non-ctor methods.
		// 1.1 in addition, prune all fields written to by a ctor method, but not the ctor of their own class. 
		// 2. prune all fields that are written to by both a ctor and a static initializer.
		// 3 prune all fields that are written to by ctors which leak 'this'. 
		// the rest of the fields are immutable. 

		// note: take care of the class hierarchy. say Circle derives from Shape.
		// if Shape.f is mutable, then so is Circle.f.
		// if Circle.f is mutable, then so is Shape.f if it exists.

		database = FieldsMutabilityDatabase.getInstance();

		findAllFieldsOfAllClasses();
		pruneAllFieldsWrittenToByNonCtors();
		pruneAllFieldsWrittenToByMultipleCtors();
		pruneAllFieldsOfObjectsWithLeakyCtors();
		printAllImmutableFields();
		
		ImmutableFieldsAccessesTagger tagger = new ImmutableFieldsAccessesTagger();
		tagger.tag();
	}

	private void pruneAllFieldsOfObjectsWithLeakyCtors() {
		Pruner3 pruner3 = new Pruner3();
		pruner3.prune();
	}

	private void pruneAllFieldsWrittenToByMultipleCtors() {
		Pruner2 pruner2 = new Pruner2();
		pruner2.prune();
	}

	private void printAllImmutableFields() {
		List<SootField> allImmutableFields = database.getAllImmutableFields();
		Logger.println("IF: Immutable fields: " + allImmutableFields);
	}

	private void pruneAllFieldsWrittenToByNonCtors() {
		Pruner pruner = new Pruner();
		pruner.prune();

	}

	private void findAllFieldsOfAllClasses() {
		AllFieldsFinder finder = new AllFieldsFinder();
		finder.find();
	}

	public static Transformer v() {
		return instance;
	}

}
