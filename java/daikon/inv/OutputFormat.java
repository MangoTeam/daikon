package daikon.inv;
import daikon.*;
import daikon.VarInfoName.Slice;
import daikon.VarInfoName.Elements;
import daikon.VarInfoName.Simple;
import daikon.VarInfoName.QuantHelper.FreeVar;
import daikon.VarInfoName.SizeOf;
import daikon.VarInfoName.FunctionOf;
import daikon.VarInfoName.Field;
import daikon.VarInfoName.TypeOf;
import daikon.VarInfoName.Add;
import daikon.VarInfoName.Subscript;
import java.util.*;

/**
 * Enumeration type for output style.
 * (Should this be somewhere else?)
 **/
public final class OutputFormat
{
  /** The standard, concise Daikon output format */
  public static final OutputFormat DAIKON = new OutputFormat("Daikon");
  /** Design-By-Contract for Java (used by Parasoft JContract) */
  public static final OutputFormat DBCJAVA = new OutputFormat("DBC");
  /** ESC/Java's annotation language */
  public static final OutputFormat ESCJAVA = new OutputFormat("ESC/Java");
    /** IOA language */
  public static final OutputFormat IOA = new OutputFormat("IOA");
  /** IOA language, sans invariant numbering */
  public static final OutputFormat IOATEST = new OutputFormat("IOA_test");
  /** Java boolean expression */
  public static final OutputFormat JAVA = new OutputFormat("Java");
  /** Java Modeling Language */
  public static final OutputFormat JML = new OutputFormat("JML");
  /** Simplify theorem prover */
  public static final OutputFormat SIMPLIFY = new OutputFormat("Simplify");
  /** Data Structure Repair Format */
  public static final OutputFormat REPAIR = new OutputFormat("Repair");

  /** Whole names as single C/Java style indentifiers (currently just
   * for single VarInfoNames) */
  public static final OutputFormat IDENTIFIER = new OutputFormat("Identifier");

  private final String name;
  public final String toString() { return "OutputFormat:" + name; }

  public boolean isJavaFamily() {
    return (this == DBCJAVA || this == JML ||  this == JAVA);
  }

  // Nobody should ever construct these
  private OutputFormat(String name) {
    this.name = name;
  }

  /**
   * Return the appropriate OutputFormat for the given name, or null
   * if no such OutputFormat exists.
   **/
  public static OutputFormat get(String name) {
    if (name == null) { return null; }
    if (name.compareToIgnoreCase(DAIKON.name) == 0) { return DAIKON; }
    if (name.compareToIgnoreCase(DBCJAVA.name) == 0) { return DBCJAVA; }
    if (name.compareToIgnoreCase(ESCJAVA.name) == 0) { return ESCJAVA; }
    if (name.compareToIgnoreCase("ESC") == 0) { return ESCJAVA; }
    if (name.compareToIgnoreCase(IOA.name) == 0) { return IOA; }
    if (name.compareToIgnoreCase(IOATEST.name) == 0) { return IOATEST; }
    if (name.compareToIgnoreCase(JAVA.name) == 0) { return JAVA; }
    if (name.compareToIgnoreCase(JML.name) == 0) { return JML; }
    if (name.compareToIgnoreCase(SIMPLIFY.name) == 0) { return SIMPLIFY; }
    if (name.compareToIgnoreCase(REPAIR.name) == 0) { return REPAIR; }
    if (name.compareToIgnoreCase(IDENTIFIER.name) == 0) { return IDENTIFIER; }
    return null;
  }

  /**
   * This class stores the state used to generate a repair
   * specification.  In particular, it stores quantifiers, mappings from
   * fields to relations, and mappings from variables to sets.
   **/

  static public class Repair {
    private static Repair repair;

    public static Repair getRepair() {
      if (repair==null)
        repair=new Repair();
      return repair;
    }
    Hashtable settable=new Hashtable();
    Hashtable relationtable=new Hashtable();
    Hashtable<Ppt,Definition> definitiontable=new Hashtable<Ppt,Definition>();
    int tagnumber=0;
    boolean forceset=false;
    HashMap revquantifiers=new LinkedHashMap();
    HashMap quantifiers=new LinkedHashMap(); // LinkedHashMap for deterministic output
    HashSet usednames=new HashSet();


    /** Creates a copy of the current Repair object state.  This copy
     * can be used to revert the state if a problem with the current
     * invariant is discovered. */

    public Repair createCopy(Ppt ppt) {
      Repair repair=new Repair();
      repair.settable.putAll(settable);
      repair.usednames.addAll(usednames);
      repair.relationtable.putAll(relationtable);
      repair.definitiontable.putAll(definitiontable);
      repair.tagnumber=tagnumber;
      repair.forceset=forceset;
      repair.quantifiers.putAll(quantifiers);
      repair.revquantifiers.putAll(revquantifiers);
      repair.varcount=varcount;
      if (repair.definitiontable.containsKey(ppt)) {
        Definition d=(Definition)repair.definitiontable.get(ppt);
        Definition newd=(Definition)d.clone();
        repair.definitiontable.put(ppt,newd);
      }
      return repair;
    }

    /** Sets the current Repair object. */

    public static void changeRepairObject(Repair r) {
      Repair.repair=r;
    }

    public void noForceSet() {
      forceset=false;
    }
    public void setForceSet() {
      forceset=true;
    }
    /** This method returns a the range set for the relation given by
     * the second parameter.*/
    public String getRange(Ppt ppt, String relation) {
      Definition d=(Definition)definitiontable.get(ppt);
      if (d==null)
        return null;
      return (String)d.rangetable.get(relation);
    }

    /** This method resets the quantifier table. */
    public void reset() {
      revquantifiers=new LinkedHashMap();
      quantifiers=new LinkedHashMap();
      forceset=false;
      varcount=0;
    }

    /** The repair system is designed to capture equality constriants
     * using relations.  For constraints involving local variables, we
     * may need to synthesize an "special" object.  */
    public void addSpecial() {
      appendQuantifier("s_quant","Special");
    }

    /** This method converts an array into a set, and returns the
     * set. */
    public String convertArraytoSet(Ppt ppt, String set, VarInfoName vin, VarInfo vi) {
      VarInfoName vin2=null;
      VarInfoName lower, upper;
      if (vin instanceof Elements) {
        Elements sequence = (Elements) vin;
        lower = sequence.getLowerBound();
        upper = sequence.getUpperBound();
        vin2=sequence.term;
      } else if (vin instanceof Slice) {
        Slice slice = (Slice) vin;
        lower = slice.getLowerBound();
        upper = slice.getUpperBound();
        vin2=slice.sequence.term;
      } else {
        // unreachable; placate javac
        throw new IllegalStateException();
      }

      //      String intervalset=generateRangeSet(ppt,lower,upper);
      // Unique set name based on range
      Tuple t=new Tuple(vi.name.name()+".arrayset",ppt,lower.name()+".."+upper.name());

      if (settable.containsKey(t)) {
        String setname=(String)settable.get(t);
        return setname;
      }
      String setname=generateSetName(vin2.name(),ppt);

      VarInfoName base=vin2.getBase();

      String baseset=VarInfoName.getRealSet(vi, base);

      //String set=VarInfoName.getRealSet(

      String newrule2;
      if (base!=null) {
        String genname=vin2.gen_name("s2");
        newrule2="[forall s2 in "+baseset+",for s="+lower.name()+" to "+upper.name()+
          "], true => "+genname+"[s] in "+setname+";";
      } else {
        newrule2="[for s="+lower.name()+" to "+upper.name()+
          "], true => "+vin2.name()+"[s] in "+setname+";";
      }


      appendModelRule(ppt,newrule2);
      appendSetRelation(ppt,"set "+setname+"("+getTypedef(ppt,vin2.name())+");");

      settable.put(t,setname);
      return setname;
    }

    /** This method converts an array into a relation, and returns the
     * relation. */
    public String convertArraytoRelation(Ppt ppt, VarInfoName vin, VarInfo vi) {
      VarInfoName vin2=null;
      VarInfoName lower, upper;
      if (vin instanceof Elements) {
        Elements sequence = (Elements) vin;
        lower = sequence.getLowerBound();
        upper = sequence.getUpperBound();
        vin2=sequence.term;
      } else if (vin instanceof Slice) {
        Slice slice = (Slice) vin;
        lower = slice.getLowerBound();
        upper = slice.getUpperBound();
        vin2=slice.sequence.term;
      } else {
        // unreachable; placate javac
        throw new IllegalStateException();
      }

      String intervalset=generateRangeSet(ppt,lower,upper);

      Tuple t=new Tuple(vi.name.name()+".arrayrelation",ppt,lower.name()+".."+upper.name());

      if (relationtable.containsKey(t)) {
        String relationname=(String)relationtable.get(t);
        return relationname;
      }
      String relationname=generateRelationName(vin2.name(),ppt);

      String rangeset=generateSetName("R"+vin2.name(),ppt);

      VarInfoName base=vin2.getBase();

      String baseset=VarInfoName.getRealSet(vi, base);

      //      String set=VarInfoName.getRealSet(

      String newrule;
      String newrule2;
      /*      if (base!=null) {
        String genname=vin2.gen_name("s2");
        newrule="[forall s2 in "+baseset+", for s="+lower.name()+" to "+upper.name()+
          "], true => <s,"+genname+"[s]> in "+relationname+";";
        newrule2="[forall s2 in "+baseset+",for s="+lower.name()+" to "+upper.name()+
          "], true => "+genname+"[s] in "+rangeset+";";
      } else {
        newrule="[for s="+lower.name()+ " to "+upper.name()+
          "], true => <s,"+vin2.name()+"[s]> in "+relationname+";";
        newrule2="[for s="+lower.name()+" to "+upper.name()+
          "], true => "+vin2.name()+"[s] in "+rangeset+";";
          }*/

      if (base!=null) {
        String genname=vin2.gen_name("s2");
        newrule="[forall s2 in "+baseset+", forall s in "+intervalset+
          "], true => <s,"+genname+"[s]> in "+relationname+";";
        newrule2="[forall s2 in "+baseset+",forall s in "+intervalset+
          "], true => "+genname+"[s] in "+rangeset+";";
      } else {
        newrule="[forall s in "+intervalset+
          "], true => <s,"+vin2.name()+"[s]> in "+relationname+";";
        newrule2="[forall s in "+intervalset+
          "], true => "+vin2.name()+"[s] in "+rangeset+";";
      }
      boolean nativetype=(getTypedef(ppt,vin2.name()).equals("int"));

      if (!nativetype) {
        appendModelRule(ppt,newrule2);
        appendSetRelation(ppt,"set "+rangeset+"("+getTypedef(ppt,vin2.name())+");");
      } else
        rangeset=getTypedef(ppt,vin2.name());

      Definition d=(Definition)definitiontable.get(ppt);
      d.rangetable.put(relationname,rangeset);
      String setdef=relationname+": int ->"+rangeset+";";
      appendModelRule(ppt,newrule);
      appendSetRelation(ppt,setdef);

      relationtable.put(t,relationname);
      return relationname;
    }

    /** This method generates a set contain the range [0..var] */

    public String generateRangeSet(Ppt ppt, VarInfoName lower, VarInfoName upper) {
      Tuple t=new Tuple(lower.name()+"-"+upper.name()+".rangeset",ppt);

      if (settable.containsKey(t)) {
        String setname=(String)settable.get(t);
        return setname;
      }
      String setname=generateSetName("Range",ppt);
      String newrule="[for i="+lower.name()+" to "+upper.name()+"], true => i in "+setname+";";
      String setdef="set "+setname+"(int);";
      appendModelRule(ppt,newrule);
      appendSetRelation(ppt,setdef);

      if (lower.name().indexOf(".")!=-1) {
        Set<VarInfoName> roots=getRoot(lower);
        for(Iterator it=roots.iterator();it.hasNext();) {
          String lowerrootvar=((VarInfoName)it.next()).name();
          String vardef="";
          if (!getType(ppt,lowerrootvar).equals("int"))
            vardef=getType(ppt,lowerrootvar)+" "+lowerrootvar+";";
          else
            vardef="int "+lowerrootvar+";";
          appendGlobal(ppt,vardef);
        }
      }

      if (upper.name().indexOf(".")!=-1) {
        Set<VarInfoName> roots=getRoot(upper);
        for(Iterator it=roots.iterator();it.hasNext();) {
          String upperrootvar=((VarInfoName)it.next()).name();
          String vardef="";
          if (!getType(ppt,upperrootvar).equals("int"))
            vardef=getType(ppt,upperrootvar)+" "+upperrootvar+";";
          else
            vardef="int "+upperrootvar+";";
          appendGlobal(ppt,vardef);
        }
      }

      settable.put(t,setname);
      return setname;
    }

    int varcount=0;
    public String getQuantifierVar(String var) {
      //      if (!quantifiers.containsKey(var)) {
        return var;
        //      } else
        //        return var+(varcount++);
    }

    public String getQuantifierVar() {
      return getQuantifierVar("i");
    }

    /** This method generates the current quantifier string. */
    public String getQuantifiers() {
      String str="";
      for(Iterator it=quantifiers.entrySet().iterator();it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        String key = (String) entry.getKey();
        String value = (String) entry.getValue();

        if (!str.equals(""))
          str+=",";
        str+="forall "+key+" in "+((String)quantifiers.get(key));
      }
      return str;
    }

    /** This method appends a quantifier to the quantifier list. */
    public void appendQuantifier(String q, String set) {
      if (!quantifiers.containsKey(q)) {
        quantifiers.put(q,set);
        revquantifiers.put(set,q);
      }
    }

    public String checkQuantifierVar(String set) {
      if (revquantifiers.containsKey(set))
        return (String)revquantifiers.get(set);
      else
        return null;
    }

    /** This method takes in a program point, the domain set, a
     * field, and the full field name fld, and returns the
     * corresponding relation. */

    public String getRelation(Ppt ppt, String set, String field, String fld) {
      Tuple t=new Tuple(set + "///" + field,ppt);
      if (relationtable.containsKey(t))
        return (String)relationtable.get(t);
      String relationname=generateRelationName(field,ppt);
      String rangeset=generateSetName("R"+field,ppt);
      String newrule="[forall s in "+set+"], true => <s,s."+field+"> in "+relationname+";";
      String newrule2="[forall s in "+set+"], true => s."+field+" in "+rangeset+";";
      boolean nativetype=getTypedef(ppt,fld).equals("int");

      if (!nativetype) {
        appendSetRelation(ppt,"set "+rangeset+"("+getTypedef(ppt,fld)+");");
        appendModelRule(ppt,newrule2);
      } else
        rangeset=getTypedef(ppt,fld);

      Definition d=(Definition)definitiontable.get(ppt);
      d.rangetable.put(relationname,rangeset);
      String setdef=relationname+": "+set+"->"+rangeset+";";
      appendModelRule(ppt,newrule);
      appendSetRelation(ppt,setdef);
      relationtable.put(t,relationname);
      return relationname;
    }

    /** This method takes in a local program variable and a program
     * point, and returns a relation. */

    public String getRelation(String programvar, Ppt ppt) {
      Tuple t=new Tuple(programvar,ppt);
      if (relationtable.containsKey(t))
        return (String)relationtable.get(t);
      String relationname=generateRelationName(programvar,ppt);

      Tuple t2=new Tuple(programvar,ppt);
      boolean generatesetdef=true;
      if (settable.containsKey(t2))
        generatesetdef=false;
      String setname=generateSetName(programvar,ppt);
      String newrule="[forall s in Special], true => <s,"+programvar+"> in "+relationname+";";

      appendModelRule(ppt,newrule);

      if (generatesetdef&&
          getTypedef(ppt,programvar).equals("int")) {
        generatesetdef=false;
        setname=getTypedef(ppt,programvar);
      }

      if (generatesetdef) {
        //Generate set definition
        String setdef="set "+setname+"("+getTypedef(ppt, programvar)+");";
        appendSetRelation(ppt,setdef);
        settable.put(t,setname);
        String newruleset="[forall s in Special], true => "+programvar+" in "+setname+";";
        appendModelRule(ppt,newruleset);
      }

      {
        //Generate relation definition
        String relationdef=relationname+": Special->"+setname+";";
        appendSetRelation(ppt,relationdef);
      }

      {
        //Generate global definition
        String vardef="";
        if (!getType(ppt,programvar).equals("int"))
          vardef=getType(ppt,programvar)+" "+programvar+";";
        else
          vardef="int "+programvar+";";
        appendGlobal(ppt,vardef);
      }

      if (!definitiontable.containsKey(ppt)) {
        definitiontable.put(ppt,new Definition());
      }
      Definition d=(Definition)definitiontable.get(ppt);
      if (!d.generatespecial) {
        //Generate special set
        d.generatespecial=true;
        appendSetRelation(ppt,"set Special(int);");
        appendModelRule(ppt,"[],true => 0 in Special;");
      }
      //store relation in table and return it
      relationtable.put(t,relationname);
      return relationname;
    }

    /** This method takes in a program variable and a program point
     * and returns the corresponding setname or a variable that
     * quantifies over that set. */

    public String getSet(String programvar, Ppt ppt) {
      String setname=getRealSet(programvar,ppt);
      if (forceset)
        return setname;
      else {
        String qvar=checkQuantifierVar(setname);
        if (qvar!=null) {
          // Just reuse the old variable...its find for a singleton set, and looks better
          return qvar;
        } else {
          String quantifiervar=getQuantifierVar(escapeString(programvar));
          appendQuantifier(quantifiervar,setname);
          return quantifiervar;
        }
      }
    }

    /** This method takes in a program variable and a program point
     * and returns the corresponding setname. */

    public String getRealSet(String programvar, Ppt ppt) {
      Tuple t=new Tuple(programvar,ppt);

      if (settable.containsKey(t)) {
        String setname=(String)settable.get(t);
        return setname;
      }
      String setname=generateSetName(programvar,ppt);
      String newrule="[], true => "+programvar+" in "+setname+";";
      String setdef="set "+setname+"("+getTypedef(ppt, programvar)+");";
      appendModelRule(ppt,newrule);
      appendSetRelation(ppt,setdef);
      String vardef="";
      if (!getType(ppt,programvar).equals("int"))
        vardef=getType(ppt,programvar)+" "+programvar+";";
      else
        vardef="int "+programvar+";";
      appendGlobal(ppt,vardef);

      settable.put(t,setname);
      return setname;
    }

    /** This method returns the roots of a VarInfoName. */

    public static Set<VarInfoName> getRoot(VarInfoName vi) {
      while(true) {
        if (vi instanceof Simple) {
          HashSet<VarInfoName> hs=new HashSet<VarInfoName>();
          hs.add(vi);
          return hs;
        } else if (vi instanceof FreeVar) {
          HashSet<VarInfoName> hs=new HashSet<VarInfoName>();
          hs.add(vi);
          return hs;
        } else if (vi instanceof SizeOf) {
          vi=((SizeOf)vi).sequence;
        } else if (vi instanceof FunctionOf) {
          vi=((FunctionOf)vi).argument;
        } else if (vi instanceof Field) {
          vi=((Field)vi).term;
        } else if (vi instanceof TypeOf) {
          vi=((TypeOf)vi).term;
        } else if (vi instanceof Add) {
          vi=((Add)vi).term;
        } else if (vi instanceof Elements) {
          vi=((Elements)vi).term;
        } else if (vi instanceof Subscript) {
          Set<VarInfoName> a=getRoot(((Subscript)vi).sequence);
          a.addAll(getRoot(((Subscript)vi).index));
          return a;
        } else if (vi instanceof Slice) {
          Set<VarInfoName> a=getRoot(((Slice)vi).sequence);
          a.addAll(getRoot(((Slice)vi).i));
          a.addAll(getRoot(((Slice)vi).j));
          return a;
        } else {
          System.out.println("Unrecognized var: "+vi.name());
          Set<VarInfoName> a=new HashSet<VarInfoName>();
          return a;
        }
      }
    }

    /** This method returns the model definition rules for a given
     * program point. */

    public String getRules(Ppt ppt) {
      Definition d=(Definition)definitiontable.get(ppt);
      if (d==null)
        return null;
      return d.modelrule;
    }

    /** This method returns the global definitions for a given program
     * point. */

    public String getGlobals(Ppt ppt) {
      Definition d=(Definition)definitiontable.get(ppt);
      if (d==null)
        return null;
      return d.globaldecls;
    }

    /** This method returns the set and relation definitions for a
     * given program point. */

    public String getSetRelation(Ppt ppt) {
      Definition d=(Definition)definitiontable.get(ppt);
      if (d==null)
        return null;
      return d.setrelation;
    }

    /** This method returns the type of a variable. */

    static public String getType(Ppt ppt, String var) {
      VarInfo vi=null;
      if (var.indexOf("size(")==0)
        return "int";
      try {
        vi=ppt.findVar(var);
      } catch (Exception e) {
        e.printStackTrace();
        return "$error";
      }
      if (vi==null) {
        System.out.println("Unknown var: "+var);
        return "$unknown_var";
      }

      if (vi.type==null)
        return "$unknown_type";
      String str;

      if (vi.file_rep_type.toString().equals("int"))
        str="int";
      else if (vi.file_rep_type.toString().equals("int[]"))
        str="int[]";
      else
        str=vi.type.toString();

      while(str.indexOf("[]")!=-1) {
        int location=str.indexOf("[]");
        str=str.substring(0,location)+"*"+str.substring(location+2,str.length());
      }
      if (str.equals("char"))
        return "byte";

      return str;
    }

    /** This method returns the type of a variable once
     * dereferenced. */

    static public String getTypedef(Ppt ppt, String var) {
      String str=getType(ppt,var);
      int last=str.lastIndexOf("*");
      if (last!=-1) {
        return str.substring(0,last);
      } else return str;
    }

    /** This method generates a set name from a program variable. */

    private String generateSetName(String programvar, Ppt p) {
      String setnameprefix="S"+programvar;
      String setname=setnameprefix;
      tagnumber=0;
      while(true) {
        Tuple t=new Tuple(setname, p);
        if (usednames.contains(t)) {
          tagnumber++;
          setname=setnameprefix+tagnumber;
        } else {
          usednames.add(t);
          break;
        }
      }
      return escapeString(setname);
    }

    /** This method generates a relation name from a program
     * variable. */
    private String generateRelationName(String fieldvar, Ppt p) {
      String relnameprefix="R"+fieldvar;
      String relname=relnameprefix;
      tagnumber=0;
      while(true) {
        Tuple t=new Tuple(relname, p);
        if (usednames.contains(t)) {
          tagnumber++;
          relname=relnameprefix+tagnumber;
        } else {
          usednames.add(t);
          break;
        }
      }
      return escapeString(relname);
    }

    /** This method appents a set or relation definition for a given
     * program point. */

    void appendSetRelation(Ppt p, String s) {
      if (!definitiontable.containsKey(p)) {
        definitiontable.put(p,new Definition());
      }
      Definition d=(Definition)definitiontable.get(p);
      d.appendSetRelation(s);
    }

    /** This method appends a model definition rule for a given
     * program point. */

    void appendModelRule(Ppt p, String s) {
      if (!definitiontable.containsKey(p)) {
        definitiontable.put(p,new Definition());
      }
      Definition d=(Definition)definitiontable.get(p);
      d.appendModelRule(s);
    }


    /** This method appends a global definition for a given program
     * point. */
    void appendGlobal(Ppt p, String s) {
      if (!definitiontable.containsKey(p)) {
        definitiontable.put(p,new Definition());
      }
      Definition d=(Definition)definitiontable.get(p);
      d.appendGlobal(s);
    }

    public static String escapeString(String s) {
      s=s.replace('.','_');
      s=s.replace('(','_');
      s=s.replace(')','_');
      s=s.replace('[','_');
      s=s.replace(']','_');
      return s;
    }

    // /** Generic tuple class.  Implements hashcode and equals.  */
     public static class Tuple {
       Object a;
       Object b;
       Object c;

       Tuple(Object a, Object b) {
         this.a=a;
         this.b=b;
         this.c=null;
       }
       Tuple(Object a, Object b,Object c) {
         this.a=a;
         this.b=b;
         this.c=c;
       }
       public int hashCode() {
         int h=a.hashCode()^b.hashCode();
         if (c!=null)
           h^=c.hashCode();
         return h;
       }
       public boolean equals(Object o) {
         if (!((o instanceof Tuple)&&
               ((Tuple)o).a.equals(a)&&
               ((Tuple)o).b.equals(b)))
           return false;
         if (c==null&&((Tuple)o).c==null)
           return true;
         if (c==null||((Tuple)o).c==null)
           return false;
         return ((Tuple)o).c.equals(c);
       }
     }

    public static class SetRelationPair {
      String set;
      String relation;
      public SetRelationPair(String s, String r) {
        set=s;
        relation=r;
      }
    }

    /** This class stores information on a given program point. */

    public static class Definition implements Cloneable {
      String setrelation="";
      String modelrule="";
      String globaldecls="";
      boolean generatespecial=false;

      Hashtable<String,String> rangetable=new Hashtable<String,String>();
      HashSet<String> globaltable=new HashSet<String>();

      void appendGlobal(String g) {
        /* Ensure that we haven't already defined the global. */
        if (!globaltable.contains(g)) {
          globaltable.add(g);
          globaldecls+=g+"\n";
        }
      }

      void appendSetRelation(String sr) {
        setrelation+=sr+"\n";
      }
      void appendModelRule(String mr) {
        modelrule+=mr+"\n";
      }

      public Definition clone() {
        Definition newd;
        try {
          newd=(Definition)super.clone();
        } catch (CloneNotSupportedException e) {
          // Can't happen because Definition directly extends Object
          throw new Error("This can't happen: " + e.toString());
        }
        rangetable = (Hashtable<String,String>) rangetable.clone(); // unchecked cast
        globaltable = (HashSet<String>) globaltable.clone(); // unchecked cast
        return newd;
      }

      // Old implementation
      // public Definition clone() {
      //   Definition newd=new Definition();
      //   newd.setrelation=setrelation;
      //   newd.modelrule=modelrule;
      //   newd.globaldecls=globaldecls;
      //   newd.generatespecial=generatespecial;
      //   newd.rangetable.putAll(rangetable);
      //   newd.globaltable.addAll(globaltable);
      //   return newd;
      // }
    }
  }
}
