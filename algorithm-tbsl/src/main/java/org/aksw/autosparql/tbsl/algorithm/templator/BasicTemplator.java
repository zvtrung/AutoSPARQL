package org.aksw.autosparql.tbsl.algorithm.templator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.autosparql.commons.nlp.pos.PartOfSpeechTagger;
import org.aksw.autosparql.commons.nlp.pos.StanfordPartOfSpeechTagger;
import org.aksw.autosparql.tbsl.algorithm.converter.DRS2BasicSPARQL_Converter;
import org.aksw.autosparql.tbsl.algorithm.converter.DUDE2UDRS_Converter;
import org.aksw.autosparql.tbsl.algorithm.ltag.parser.LTAGLexicon;
import org.aksw.autosparql.tbsl.algorithm.ltag.parser.LTAG_Lexicon_Constructor;
import org.aksw.autosparql.tbsl.algorithm.ltag.parser.Parser;
import org.aksw.autosparql.tbsl.algorithm.ltag.parser.Preprocessor;
import org.aksw.autosparql.tbsl.algorithm.sem.drs.DRS;
import org.aksw.autosparql.tbsl.algorithm.sem.drs.UDRS;
import org.aksw.autosparql.tbsl.algorithm.sem.dudes.data.Dude;
import org.aksw.autosparql.tbsl.algorithm.sem.dudes.reader.ParseException;
import org.aksw.autosparql.tbsl.algorithm.sparql.BasicQueryTemplate;
import org.aksw.autosparql.tbsl.algorithm.sparql.Slot;

public class BasicTemplator {

	String[] GRAMMAR_FILES = {"tbsl/lexicon/basic_english.lex"};

	PartOfSpeechTagger tagger;
	LTAGLexicon g;
	LTAG_Lexicon_Constructor LTAG_Constructor = new LTAG_Lexicon_Constructor();
	Parser p;
	Preprocessor pp;

    DUDE2UDRS_Converter d2u;
    DRS2BasicSPARQL_Converter d2s;

	boolean ONE_SCOPE_ONLY = true;
	public boolean UNTAGGED_INPUT = true;

	public BasicTemplator() {

            List<InputStream> grammarFiles = new ArrayList<InputStream>();
            for(int i = 0; i < GRAMMAR_FILES.length; i++){
                grammarFiles.add(this.getClass().getClassLoader().getResourceAsStream(GRAMMAR_FILES[i]));
            }

            g = LTAG_Constructor.construct(grammarFiles);

            tagger = StanfordPartOfSpeechTagger.INSTANCE;
//          tagger = new ApachePartOfSpeechTagger();

	    p = new Parser();
	    p.SHOW_GRAMMAR = true;
	    p.USE_DPS_AS_INITTREES = true;
	    p.CONSTRUCT_SEMANTICS = true;
	    p.MODE = "BASIC";

	    pp = new Preprocessor(false);

	    d2u = new DUDE2UDRS_Converter();
	    d2s = new DRS2BasicSPARQL_Converter();
	}

        public void setGrammarFiles(String[] files) {
            GRAMMAR_FILES = files;
            List<InputStream> grammarFiles = new ArrayList<InputStream>();
            for(int i = 0; i < GRAMMAR_FILES.length; i++){
                grammarFiles.add(this.getClass().getClassLoader().getResourceAsStream(GRAMMAR_FILES[i]));
            }
        }

	public void setUNTAGGED_INPUT(boolean b) {
		UNTAGGED_INPUT = b;
	}

	public Set<BasicQueryTemplate> buildBasicQueries(String s) {

		boolean clearAgain = true;

		String tagged;
		if (UNTAGGED_INPUT) {
			s = pp.normalize(s);
			tagged = tagger.tag(s);
			System.out.println("Tagged input: " + tagged);
		}
		else {
			tagged = s;
		}

		String newtagged = pp.condenseNominals(tagged);
		newtagged = pp.condense(newtagged);
		System.out.println("Preprocessed: " + newtagged);

        p.parse(newtagged,g);

        if (p.getDerivationTrees().isEmpty()) {
            p.clear(g,p.getTemps());
            clearAgain = false;
            System.out.println("[BasicTemplator.java] '" + s + "' could not be parsed.");
        }
        else {
        try {
        	p.buildDerivedTrees(g);
        } catch (ParseException e) {
            System.err.println("[BasicTemplator.java] ParseException at '" + e.getMessage() + "'");
        }
        }

        Set<DRS> drses = new HashSet<DRS>();
        Set<BasicQueryTemplate> querytemplates = new HashSet<BasicQueryTemplate>();

        for (Dude dude : p.getDudes()) {
            UDRS udrs = d2u.convert(dude);
            if (udrs != null) {

            	for (DRS drs : udrs.initResolve()) {

                	List<Slot> slots = new ArrayList<Slot>();
            		slots.addAll(dude.getSlots());
            		d2s.setSlots(slots);
                	d2s.redundantEqualRenaming(drs);

                	if (!containsModuloRenaming(drses,drs)) {
                    	// DEBUG
                		System.out.println("\nDUDE:\n" + dude);
                		System.out.println("\nDRS:\n" + drs);
                		for (Slot sl : slots) {
                			System.out.println(sl.toString());
                		}
                		//
                		drses.add(drs);

                		try {
                			BasicQueryTemplate qtemp = d2s.convert(drs,slots);
                			querytemplates.add(qtemp);
                		} catch (java.lang.ClassCastException e) {
                			continue;
                		}
                		if (ONE_SCOPE_ONLY) { break; }
                	}
                }
            }
        }

        if (clearAgain) {
        	p.clear(g,p.getTemps());
        }
        System.gc();

        return querytemplates;
    }

	private boolean containsModuloRenaming(Set<DRS> drses, DRS drs) {

		for (DRS d : drses) {
			if (d.equalsModuloRenaming(drs)) {
				return true;
			}
		}
		return false;
	}

}
