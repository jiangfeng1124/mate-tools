package se.lth.cs.srl.corpus;

import is2.data.SentenceData09;
import is2.io.CONLLReader09;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class Sentence extends ArrayList<Word> {

	private static final Pattern WHITESPACE_PATTERN=Pattern.compile("\\s+");
	
	private static final long serialVersionUID = 10;
	
	private List<Predicate> predicates;
	private Sentence() { 
		Word BOS=new Word(this);
		super.add(BOS); //Add the root token
		predicates=new ArrayList<Predicate>();
	}

	public Sentence(SentenceData09 data){
		this(data.forms,data.lemmas,data.ppos,data.pfeats);
	}
	
	public Sentence(String[] words, String[] lemmas, String[] tags,	String[] morphs) {
		this();
		for(int i=0;i<words.length;++i){
			Word nextWord=new Word(words[i],lemmas[i],tags[i],morphs[i],this);
			super.add(nextWord);
		}
	}

	private void addPredicate(Predicate pred){
		predicates.add(pred);
	}
	
	public List<Predicate> getPredicates() {
		return predicates;
	}
	
	public void buildDependencyTree(){
		for(int i=1;i<size();++i){
			Word curWord = get(i);
			curWord.setHead(get(curWord.getHeadId()));
		}		
	}
	
	public void buildSemanticTree(){
		for(int i=0;i<predicates.size();++i){
			Predicate pred=predicates.get(i);
			for(int j=1;j<super.size();++j){
				Word curWord = get(j);
				String arg=curWord.getArg(i);
				if(!arg.equals("_"))
					pred.addArgMap(curWord,arg);
			}
		}
		for(Word w:this) //Free this memory as we no longer need this string array
			w.clearArgArray();		
	}
	
	public String toString() {
		String tag;
		StringBuilder ret=new StringBuilder();
		for(int i=1;i<super.size();++i){
			Word w=super.get(i);
			ret.append(i).append("\t").append(w.toString());
			if(!(w instanceof Predicate)) //If its not a predicate add the FILLPRED and PRED cols
				ret.append("\t_\t_");
			for(int j=0;j<predicates.size();++j){
				ret.append("\t");
				Predicate pred=predicates.get(j);
				ret.append((tag=pred.getArgumentTag(w))!=null?tag:"_");
			}
			ret.append("\n");
		}
		return ret.toString().trim();
	}
	
	public void makePredicate(int wordIndex) {
		Predicate p=new Predicate(super.get(wordIndex));
		super.set(wordIndex, p);
		addPredicate(p);
	}
	
	/*
	 * Functions used when interfacing with Bohnets parser
	 */
	public String[] getFormArray(){
		String[] ret=new String[this.size()];
		ret[0]="<root>";
		for(int i=1;i<this.size();++i)
			ret[i]=this.get(i).Form;
		return ret;
	}
	public String[] getPOSArray(){
		String[] ret=new String[this.size()];
		ret[0]="<root-POS>";
		for(int i=1;i<this.size();++i)
			ret[i]=this.get(i).POS;
		return ret;
	}
	public String[] getFeats(){
		String[] ret=new String[this.size()];
		ret[0]=CONLLReader09.NO_TYPE;
		for(int i=1;i<this.size();++i)
			ret[i]=this.get(i).getFeats();
		return ret;
	}
	public void setHeadsAndDeprels(int[] heads,String[] deprels){
		for(int i=0;i<heads.length;++i){
			Word w=this.get(i+1);
			w.setHead(this.get(heads[i]));
			w.setDeprel(deprels[i]);
		}
	}
	public static Sentence newDepsOnlySentence(String[] lines) {
		Sentence ret=new Sentence();
		Word nextWord;
		for(String line:lines){
			String[] cols=WHITESPACE_PATTERN.split(line,13);
			nextWord=new Word(cols,ret);
			ret.add(nextWord);
		}
		ret.buildDependencyTree();
		return ret;
		
	}
	public static Sentence newSentence(String[] lines){
		Sentence ret=new Sentence();
		Word nextWord;
		for(String line:lines){
			String[] cols=WHITESPACE_PATTERN.split(line);
			if(cols[12].equals("Y")){
				Predicate pred=new Predicate(cols,ret);
				ret.addPredicate(pred);
				nextWord=pred;
			} else {
				nextWord=new Word(cols,ret);
			}
			ret.add(nextWord);	
		}
		ret.buildDependencyTree();
		ret.buildSemanticTree();
		return ret;
	}
	public static Sentence newSRLOnlySentence(String[] lines){
		Sentence ret=new Sentence();
		Word nextWord;
		for(String line:lines){
			String[] cols=WHITESPACE_PATTERN.split(line,13);
			if(cols[12].charAt(0)=='Y'){
				Predicate pred=new Predicate(cols,ret);
				ret.addPredicate(pred);
				nextWord=pred;
			} else {
				nextWord=new Word(cols,ret);
			}
			ret.add(nextWord);	
		}
		ret.buildDependencyTree();
		return ret;		
	}
	
	public final Comparator<Word> wordComparator=new Comparator<Word>(){
		@Override
		public int compare(Word arg0, Word arg1) {
			return indexOf(arg0) - indexOf(arg1);
		}		
	};
}