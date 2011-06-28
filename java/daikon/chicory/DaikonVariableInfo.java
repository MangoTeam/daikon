package daikon.chicory;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import daikon.util.*;

import daikon.Chicory;


/**
 * Each DaikonVariableInfo object is a node in the tree structure of the
 * variables in the target application.  The tree structure is built in the
 * DeclWriter and traversed in the DTraceWriter.  There is such a tree
 * structure associated with every program point.  This architecture makes
 * it possible to avoid the issue of "traversal pattern duplication" in
 * which both the DeclWriter and DTraceWriter must traverse the target
 * application's variables identically.  In general, the variable a will be
 * the parent of the variables a.b and a.c in the tree, where b and c are
 * fields in a's class.
 *
 * Each node can have any non-negative number of child nodes.
 * DaikonVariableInfo is an abstract class.  Its subtypes are designed to
 * represent specific types of variables, such as arguments, arrays, etc.
 */
public abstract class DaikonVariableInfo
    implements Iterable<DaikonVariableInfo>, Comparable<DaikonVariableInfo>
{

    /** Enable experimental techniques on static constants. */
    public static boolean dkconfig_constant_infer = false;

    /** The variable name.  Sensible for all subtypes except RootInfo.  **/
    private final /*@Interned*/ String name;

    /** The child nodes **/
    public List<DaikonVariableInfo> children;

    /** True iff this variable is an array **/
    protected final boolean isArray;

    /** Print debug information about the variables **/
    static SimpleLog debug_vars = new SimpleLog (false);

    private static SimpleLog debug_array = new SimpleLog (true);

    /**default string for comparability info**/
    private static final String compareInfoDefaultString = "22";

    /**used to assert that a given variable is a parameter to a method**/
    protected static final String isParamString = " # isParam=true";

    // Certain hardcoded class names
    protected static final String classClassName = "java.lang.Class";
    protected static final String stringClassName = "java.lang.String";

    /** Suffix for "typeOf" variables that represent a class, eg, "foo.getClass()". **/
    public static final String class_suffix = ".getClass()";

    /** Determines whether or not synthetic variables should be ignored **/
    private static boolean skip_synthetic = true;

    /**
     * The printed type that will appear in the .decls declaration.  May
     * include aux information at the end, such as isParamString.
     * @see #getTypeName()
     * @see #getTypeNameOnly()
     */
    protected String typeName; 
    /**
     * The printed representation type that will appear in the .decls
     * declaration.
     */
    protected String repTypeName;
    /**
     * The printed comparability information that will appear in the .decls
     * declaration.
     */
    protected String compareInfoString = compareInfoDefaultString;

    /** Value of static constants.  Access via {@link #get_const_val} method. **/
    /*@Nullable*/ String const_val = null;
    
    /** Arguments used to create a function. Access via {@link #get_func_args()} method. **/
    /*@Nullable*/ String function_args = null;
    
    /** True iff the DeclWriter should print this variable **/
    protected boolean declShouldPrint = true;

    /** True iff the DTraceWriter should print this variable **/
    protected boolean dtraceShouldPrint = true;

    /** True iff the DTraceWriter should print the children of this variable **/
    protected boolean dtraceShouldPrintChildren = true;

    /**
     * If false, use standard dfej behavior (any field in an
     * instrumented class is visible).  If true, use standard Java
     * behavior (if the field is in a class in a different package, it
     * is only visible if public, etc.).
     */
    public static boolean std_visibility = false;

    /**
     * Set of fully qualified static variable names for this ppt.  Used
     * to ensure that each static is only included once (regardless of
     * how many other variables may include its declaring class).
     */
    protected static Set<String> ppt_statics = new LinkedHashSet<String>();

    /** Constructs a non-array type DaikonVariableInfo object
     * @param theName The name of the variable
     */
    public DaikonVariableInfo(String theName, String typeName, String repTypeName)
    {
        this(theName, typeName, repTypeName, false);
    }

    /**
     * Constructs a DaikonVariableInfo object
     * @param theName The variable's name
     * @param arr True iff the variable is an array
     */
    public DaikonVariableInfo(String theName, String typeName, String repTypeName, boolean arr)
    {
        // Intern the names because there will be many of the
        // same variable names at different program points within
        // the same class.
        name = theName.intern();
        this.typeName = typeName.intern();
        this.repTypeName = repTypeName.intern();

        children = new ArrayList<DaikonVariableInfo> ();
        isArray = arr;

        if ((theName != null) &&
            (theName.contains ("[..]") || theName.contains ("[]")) && !isArray)
            debug_array.log_tb ("%s is not an array", theName);

     }

    /**
     * Returns the name of this variable.
     */
    public /*@Nullable*/ String getName()
    {
        if (name == null)
            return null;

        if (Chicory.new_decl_format)
            return name.replaceFirst ("\\[]", "[..]");
        else
            return name;
    }

    /**
     * Add a child to this node.
     * Should only be called while the tree is being constructed.
     *
     * @param info The child object, must be non-null.  The child's
     * fields name, typeName, repTypeName, and compareInfoString
     * should also be non-null.
     */
    protected void addChild(DaikonVariableInfo info)
    {
        assert info!=null : "info cannot be null in DaikonVariableInfo.addChild()";
        assert info.name != null : "Child's name should not be null";
        assert info.typeName != null : "Child's type name should not be null";
        assert info.repTypeName != null : "Child's representation type name should not be null";
        assert info.compareInfoString != null : "Child's comparability information should not be null";

        debug_vars.log ("Adding %s to %s\n", info, this);
        children.add(info);
    }

    /**
     * Returns a string representation of this node.
     */
    public String toString()
    {
        return getClass().getName() + ":" + getName();
    }

    /** Returns a string representative of this node and its children **/
    public String treeString()
    {
        return getStringBuffer(new StringBuffer("--")).toString();
    }

    /**
     * Return a StringBuffer which contains the name of this node
     * and all ancestors of this node.
     * Longer indentations correspond to further distance in the tree.
     *
     * @param offset The offset to begin each line with.
     * @return StringBuffer which contains all children of this node
     */
    private StringBuffer getStringBuffer(StringBuffer offset)
    {
        StringBuffer theBuf = new StringBuffer();

        theBuf.append(offset + name + DaikonWriter.lineSep);

        StringBuffer childOffset = new StringBuffer(offset);
        childOffset.append("--");
        for (DaikonVariableInfo info: children)
        {
            theBuf.append(info.getStringBuffer(childOffset));
        }

        return theBuf;
    }

    /**
     * Return an iterator over all the node's children.
     * Don't modify the list of children through the iterator,
     * as an unmodifiable list is used to generate the iterator.
     */
    public Iterator<DaikonVariableInfo> iterator()
    {
        return Collections.unmodifiableList(children).iterator();
    }

    /** Returns the complete tree of variables as a list **/
    public List<DaikonVariableInfo> tree_as_list()
    {
        List<DaikonVariableInfo> list = new ArrayList<DaikonVariableInfo>();
        list.add (this);
        for (DaikonVariableInfo dv : children)
        {
            list.addAll (dv.tree_as_list());
        }
        return (list);
    }

    /**
     * Given an object value corresponding to the parent of this
     * DaikonVariableInfo variable, return the value (of the corresponding
     * value in the target application) of this DaikonVariableInfo variable.
     *
     * For instance, if the variable a has a field b, then calling
     * getMyValFromParentVal(val_of_a) will return the value of a.b
     *
     * @param parentVal The parent object.  Can be null for static fields.
     *    (Are there any other circumstances where it can be null?)
     */
    public abstract /*@Nullable*/ Object getMyValFromParentVal(Object parentVal);

    /**
     * Returns a String representation of this object suitable for a .dtrace file
     * @param val The object whose value to print
     */
    @SuppressWarnings("unchecked")
    public String getDTraceValueString(Object val)
    {
        if (isArray)
        {
            return getValueStringOfListWithMod((List<Object>) val); // unchecked cast
        }
        else
        {
            return getValueStringOfObjectWithMod(val, true);
        }
    }

    /**
     *
     * Gets the value of an object and concatenates
     * the associated "modified" integer.
     */
    protected String getValueStringOfObjectWithMod(Object theValue, boolean hashArray)
    {
        String retString =getValueStringOfObject(theValue, hashArray) + DaikonWriter.lineSep;

        if (theValue instanceof NonsensicalObject)
            retString += "2";
        else
            retString += "1";

        return retString;
    }

    /**
     * Gets the value, but with no endline.
     * If hashArray is true, it prints the "hash code" of the array
     * and not its separate values.
     */
    private String getValueStringOfObject(Object theValue, boolean hashArray)
    {
        if (theValue == null)
        {
            return "null";
        }

        Class<?> type = theValue.getClass();

        assert !(type.isPrimitive()) : "Objects cannot be primitive";

        if (theValue instanceof Runtime.PrimitiveWrapper)
        {
            return getPrimitiveValueString(theValue);
        }
        else if (!hashArray && (type.isArray()))
        {
            //show the full array
            return getValueStringOfArray(theValue);
        }
        else if (theValue instanceof NonsensicalObject)
        {
            return ("nonsensical");
        }
        else
        {
            //basically, show the hashcode of theValue
            return getObjectHashCode(theValue);
        }
    }

    /**
     * Get value string for a primitive (wrapped) object
     */
    private String getPrimitiveValueString(Object obj)
    {
        assert (obj instanceof Runtime.PrimitiveWrapper) : "Objects passed to showPrimitive must implement PrimitiveWrapper" + DaikonWriter.lineSep +"This object is type: " + obj.getClass().getName();

        //use wrapper classes toString methods to print value
        return (obj.toString());
    }


    /**
     * Gets a string representation of the values in an array
     */
    private String getValueStringOfArray(Object array)
    {
        List<Object> theList = DTraceWriter.getListFromArray(array);
        return getValueStringOfList(theList);
    }

    /**
     *
     *  Gets the Object's unique ID as a string.
     *  In other words, a "hash code"
     */
    private String getObjectHashCode(Object theObject)
    {
        if (theObject == null)
            return ("null");
        else if (theObject instanceof NonsensicalObject)
            return ("nonsensical");
        else
            return Integer.toString(System.identityHashCode(theObject));
    }

    /**
     *
     *  Gets the list of values (as a string) from getValueStringOfList
     *  and concatenates the "modified" value
     */
    private String getValueStringOfListWithMod(List<Object> theValues)
    {
        String retString = getValueStringOfList(theValues) + DaikonWriter.lineSep;

        if (theValues instanceof NonsensicalList)
            retString += ("2");
        else
            retString += ("1");

        return retString;
    }

    /**
     * Returns a string representation of the values
     * of a list of values as if it were an array.
     *
     * @param theValues The values to print out
     */
    protected String getValueStringOfList(List<Object> theValues)
    {
        if (theValues == null)
        {
            return ("null");
        }

        if (theValues instanceof NonsensicalList)
        {
            return ("nonsensical");
        }

        StringBuffer buf = new StringBuffer();

        buf.append("[");
        for (Iterator<Object> iter = theValues.iterator(); iter.hasNext();)
        {
            Object elementVal = iter.next();

            //hash arrays...
            //don't want to print arrays within arrays
            buf.append(getValueStringOfObject(elementVal, true));

            //put space between elements in array
            if (iter.hasNext())
                buf.append(" ");
        }
        buf.append("]");

        return buf.toString();
    }


    /**
     * Add the parameters of the given method to this node.
     */
    protected void addParameters(ClassInfo cinfo,
             Member method, List<String> argnames, String offset, int depth) {
        Class<?>[] arguments = (method instanceof Constructor<?>)
            ? ((Constructor<?>) method).getParameterTypes()
            : ((Method) method).getParameterTypes();

        Iterator<String> argnamesiter = argnames.iterator();
        int param_offset = 0;
        for (int i = 0; (i < arguments.length) && argnamesiter.hasNext(); i++)
        {
            Class<?> type = arguments[i];
            String name = argnamesiter.next();
            if (type.getName().equals ("daikon.dcomp.DCompMarker"))
                continue;
            if (type.getName().equals ("java.lang.DCompMarker"))
                continue;
            debug_vars.indent ("processing parameter '%s'%n", name);
            DaikonVariableInfo theChild = addDeclVar(cinfo, type,
                                         name, offset, depth, i, param_offset);
            param_offset++;
            if ((type == Double.TYPE) || (type == Long.TYPE))
                param_offset++;
            theChild.addChildNodes(cinfo, type, name, offset, depth);
            debug_vars.exdent();
        }
    }

    /**
     * Adds class variables (i.e., the fields) for the given type and
     * attach new nodes as children of this node.
     * @param type the class whose fields should all be added to this node
     */
    /*@NonNullOnEntry("cinfo.clazz")*/
    protected void addClassVars(ClassInfo cinfo, boolean dontPrintInstanceVars,
                                Class<?> type, String offset, int depth) {

        //DaikonVariableInfo corresponding to the "this" object
        DaikonVariableInfo thisInfo;

        //must be at the first level of recursion (not lower) to print "this" field
        if (!dontPrintInstanceVars && offset.equals(""))
        {
            // "this" variable
            thisInfo = new ThisObjInfo(type);
            addChild(thisInfo);

            //.class variable
            if (shouldAddRuntimeClass(type))
            {
                DaikonVariableInfo thisClass = new DaikonClassInfo("this.getClass()", classClassName, stringClassName, "this", false);
                thisInfo.addChild(thisClass);
            }
        }
        else if (offset.equals ("")) {
            // Create a non-printing root for static variables.
            thisInfo = new StaticObjInfo(type);
            addChild (thisInfo);
        } else {
            thisInfo = this;
        }

        // Get the fields
        // System.out.printf ("getting fields for %s%n", type);
        Field[] fields = type.getDeclaredFields();

        // if (fields.length > 50)
        //    System.out.printf ("%d fields in %s%n", fields.length, type);

        debug_vars.log ("%s: [%s] %d dontPrintInstanceVars = %b, "
                        + "inArray = %b%n", type, offset, fields.length,
                        dontPrintInstanceVars, isArray);


        for (Field classField : fields) {
            boolean is_static = Modifier.isStatic (classField.getModifiers());

            // Skip created variables (they were probably created by us)
            if (skip_synthetic && classField.isSynthetic())
                continue;

            debug_vars.log ("considering field %s -> %s%n", offset,
                            classField);

            if (!is_static && dontPrintInstanceVars)
            {
                debug_vars.log ("--field !static and instance var %b%n",
                                dontPrintInstanceVars);
                continue;
            }

            // Skip variables that match the ignore pattern
            if (Chicory.omit_var != null) {
                String fullname = offset + "." + classField.getName();
                if (is_static)
                    fullname = classField.getDeclaringClass().getName()
                        + "." + classField.getName();
                Matcher m = Chicory.omit_var.matcher (fullname);
                if (m.find()) {
                    System.out.printf ("VAR %s matches omit pattern %s%n",
                                       fullname, Chicory.omit_var);
                    continue;
                }
            }

            // Don't print arrays of the same static field
            if (is_static && isArray)
            {
                debug_vars.log ("--field static and inArray%n");
                continue;
            }

            // Skip any statics that have been already included
            if (is_static) {
                String full_name = classField.getDeclaringClass().getName()
                    + "." + classField.getName();
                if (ppt_statics.contains (full_name) && (depth <= 0)) {
                    debug_vars.log ("already included static %s (no children)",
                                    full_name);

                    continue;
                }
            }

            if (!isFieldVisible (cinfo.clazz, classField))
            {
                debug_vars.log ("--field not visible%n");
                continue;
            }

            Class<?> fieldType = classField.getType();

            StringBuffer buf = new StringBuffer();
            DaikonVariableInfo newChild = thisInfo.addDeclVar(classField, offset, buf);

            debug_vars.indent ("--Created DaikonVariable %s%n", newChild);

            String newOffset = buf.toString();
            newChild.addChildNodes(cinfo, fieldType, classField.getName(),
                          newOffset, depth);
            debug_vars.exdent();
        }


        // If appropriate, print out decls information for pure methods
        // and add to the tree
        // Check dontPrintInstanceVars is basically checking if the program point method
        // (not the pure method) is static.  If it is, don't continue because we can't
        // call instance methods (all pure methods we consider are instance methods)
        // from static methods
        if (ChicoryPremain.shouldDoPurity() && !dontPrintInstanceVars)
        {
            ClassInfo typeInfo = null;

            try
            {
                typeInfo = Runtime.getClassInfoFromClass(type);
            }
            catch (RuntimeException e)
            {
                // Could not find the class... no further purity analysis
                typeInfo = null;
            }
            
            if (typeInfo != null)
            {
                //Pure methods with no parameters
                for (MethodInfo meth : typeInfo.method_infos)
                {    
                    if (meth.isPure() && meth.arg_names.length == 0)
                    {
                        StringBuffer buf = new StringBuffer();
                        DaikonVariableInfo newChild = thisInfo.addPureMethodDecl(
                                cinfo, meth, new DaikonVariableInfo[] {}, offset,
                                depth, buf);
                        String newOffset = buf.toString();
                        debug_vars.indent ("Pure method");
                        assert meth.member != null : "@SuppressWarnings(nullness): member of method_infos have .member field"; // fix with dependent type
                        newChild.addChildNodes(cinfo,
                                               ((Method) meth.member).getReturnType(),
                                               meth.member.getName(),
                                               newOffset,
                                               depth);
                        debug_vars.exdent();

                    }
                }
                
                // List containing all class variables, excluding pure methods with parameters
                List<DaikonVariableInfo> siblings = new ArrayList<DaikonVariableInfo>(thisInfo.children);
                
                // Pure methods with one parameter
                for (MethodInfo meth : typeInfo.method_infos)
                {
                    if (meth.isPure() && meth.arg_names.length == 1)
                    {
                        for (DaikonVariableInfo sib : siblings)
                        {
                            String sibType = sib.getTypeNameOnly();
                            Class<?> sibClass = null;

                            // Get class type of the class variable
                            try
                            {
                                sibClass = UtilMDE.classForName(sibType);
                            } catch (ClassNotFoundException e)
                            {    
                            	throw new Error(e);
                            }

                            // Add node if the class variable can be used as the pure method's parameter
                            if (UtilMDE.isSubtype(sibClass, meth.arg_types[0]))
                            {
                                DaikonVariableInfo[] arg = {sib};
                                StringBuffer buf = new StringBuffer();
                                DaikonVariableInfo newChild = thisInfo.addPureMethodDecl(
                                        cinfo, meth, arg, offset, depth,
                                        buf);
                                String newOffset = buf.toString();
                                debug_vars.indent ("Pure method");
                                assert meth.member != null : "@SuppressWarnings(nullness): member of method_infos have .member field"; // fix with dependent type
                                newChild.addChildNodes(cinfo,
                                                       ((Method) meth.member).getReturnType(),
                                                       meth.member.getName(),
                                                       newOffset,
                                                       depth);
                                debug_vars.exdent();
                            }
                        }                    
                    }
                }
            }
        }
    }

    /**
     * Adds the decl info for a single parameter as a child of this node.
     * Also adds "derived" variables
     * such as the runtime .class variable.
     *
     * @return The newly created DaikonVariableInfo object, whose
     * parent is this.
     */
    protected DaikonVariableInfo addDeclVar(ClassInfo cinfo, Class<?> type,
           String name, String offset, int depth, int argNum, int param_offset)
    {
        // add this variable to the tree as a child of curNode
        DaikonVariableInfo newChild = new ParameterInfo(offset + name, argNum,
                                                        type, param_offset);

        addChild(newChild);

        boolean ignore = newChild.check_for_dup_names();
        if (!ignore)
            newChild.checkForDerivedVariables(type, name, offset);

        return newChild;
    }
    
    
    /**
     * Adds the decl info for a pure method.
     */
    //TODO factor out shared code with printDeclVar
    protected DaikonVariableInfo addPureMethodDecl(ClassInfo curClass,
            MethodInfo minfo, DaikonVariableInfo[] args, String offset,
            int depth, StringBuffer buf)
    {
        String arr_str = "";
        if (isArray)
        {
            arr_str = "[]";
        }
        
        @SuppressWarnings("nullness") // method precondition
        /*@NonNull*/ Method meth = (Method) minfo.member;


        boolean changedAccess = false;

        //we want to access all fields...
        if (!meth.isAccessible())
        {
            changedAccess = true;
            meth.setAccessible(true);
        }

        Class<?> type = meth.getReturnType();
        assert type != null;

        String theName = meth.getName() + "(";
        if(args.length > 0) {
            theName += args[0].getName();
        }
        if (args.length > 1) 
        {
            for (int i = 1; i < args.length - 1; i++) {
                theName += ", " + args[i].getName();
            }
        }
        theName += ")";

        if (offset.length() > 0) // offset already starts with "this"
        {
        }
        else
        {
            offset = "this.";
        }

        String type_name = stdClassName (type);
        // TODO: Passing incorrect receiver name????
        DaikonVariableInfo newPure = new PureMethodInfo(offset + theName, minfo,
                type_name + arr_str, getRepName(type, isArray) + arr_str, offset.substring(0, offset.length() - 1),
                isArray, args);

        addChild(newPure);

        newPure.checkForDerivedVariables(type, theName, offset);

        buf.append(offset);

        if (changedAccess)
        {
            meth.setAccessible(false);
        }

        return newPure;
    }

    /**
     * Adds the decl info for a single class variable (a field)
     * as a child of this node.  Also adds "derived" variables
     * such as the runtime .class variable.
     *
     * @return The newly created DaikonVariableInfo object, whose
     * parent is this.
     */
    protected DaikonVariableInfo addDeclVar(Field field, String offset,
                                StringBuffer buf)
    {
        String arr_str = "";
        if (isArray)
            arr_str = "[]";

        boolean changedAccess = false;

        //we want to access all fields...
        if (!field.isAccessible())
        {
            changedAccess = true;
            field.setAccessible(true);
        }

        Class<?> type = field.getType();
        String theName = field.getName();
        int modifiers = field.getModifiers();

        if (Modifier.isStatic(modifiers)) {
            offset = field.getDeclaringClass().getName() + ".";
        } else if (offset.length() == 0) {// instance fld, 1st recursion step
            offset = "this.";
        }

        String type_name = stdClassName (type) + arr_str;

        // Print auxiliary information.
        type_name += appendAuxInfo(field);

        DaikonVariableInfo newField = new FieldInfo(offset + theName, field,
                                                    type_name, getRepName(type, false) + arr_str,
                                                    isArray);
        boolean ignore = newField.check_for_dup_names();

        if (DaikonWriter.isStaticConstField(field) && !isArray)
        {
            ClassInfo cinfo = Runtime.getClassInfoFromClass(field.getDeclaringClass());
            String value = null;
            boolean isPrimitive = true;

            if (cinfo != null) {
                value = cinfo.staticMap.get(theName);

                if (DaikonVariableInfo.dkconfig_constant_infer) {
                    if (value == null) {
                        isPrimitive = false;
                        String className = field.getDeclaringClass().getName();
                        // If the class has already been statically initialized, get its hash
                        if (Runtime.isInitialized(className)) {
                            try {
                                value = Integer.toString(System.identityHashCode(field.get(null)));
                            } catch(Exception e) {}
                        }
                    }
                }
            }

            // System.out.printf ("static final value = %s%n", value);

            // in this case, we don't want to print this variable to
            // the dtrace file
            if (value != null)
            {
                newField.repTypeName += " = " + value;
                newField.const_val = value;
                newField.dtraceShouldPrint = false;
                if (DaikonVariableInfo.dkconfig_constant_infer && isPrimitive) {
                    newField.dtraceShouldPrintChildren = false;
                }
            }
            //else
            //{
                // don't print anything
                // because this field wasn't declared with an actual "hardcoded" constant
            //}

        }

        addChild(newField);

        if (!ignore)
            newField.checkForDerivedVariables(type, theName, offset);

        buf.append(offset);

        if (changedAccess)
        {
            field.setAccessible(false);
        }

        return newField;
    }

    /**
     * Returns the class name of the specified class in 'Java' format
     * (i.e., as the class would have been declared in Java source code)
     */
    public static String stdClassName (Class<?> type)
    {
        return Runtime.classnameFromJvm (type.getName());
    }

    /**
     * Given a type, gets the representation type to be used in Daikon. For
     * example, the representation type of a class object is "hashcode."
     *
     * @param type
     *            The type of the variable
     * @param asArray
     *            Whether the variable is being output as an array (true) or as
     *            a pointer (false).
     * @return The representation type as a string
     */
    public static String getRepName(Class<?> type, boolean asArray)
    {
        if (type == null)
        {
            return "hashcode";
        }
        else if (type.isPrimitive())
        {
            if (type.equals(Double.TYPE))
                return "double";
            else if (type.equals(Float.TYPE))
                return "double";
            else if (type.equals (Boolean.TYPE))
                return "boolean";
            else
                return "int";
        }
        else if (type.getName().equals("java.lang.String"))
        {
            // if we are printing the actual array, the rep type is "java.lang.String"
            if (true)
                return "hashcode";
            if (asArray)
                return "java.lang.String";
            // otherwise, it is just a hashcode
            else
                return "hashcode";
        }
        else
        {
            return "hashcode";
        }
    }

    /**
     * Determines if type needs a corresponding .class runtime class variable.
     * <p>
     * The .class variable is printed for interfaces, abstract classes, and
     * Object.  For these types, the run-time class is always (or, for
     * Object, usually) different than the declared type.  An alternate,
     * and possibly more useful, heuristic would be to print the .class
     * variable for any type that has subtypes.
     *
     * @param type
     *            The variable's Type
     */
    protected static boolean shouldAddRuntimeClass(Class<?> type)
    {
        // For some reason, abstracts seems to be set on arrays
        // and primitives.  This is a temporary fix to get things
        // close.
        if (type.isPrimitive())
            return (false);
        if (type.isArray())
        {
            Class<?> eltType = type.getComponentType();
            assert eltType != null; // because type is an array
            return !(eltType.isPrimitive());
        }

        if (type.getName().equals("java.lang.Object")) //Objects
        {
            // System.out.println ("type is object " + type);
            return true;
        }
        else if (Modifier.isAbstract(type.getModifiers()))
        {
            // System.out.printf ("Type [%s] is abstract %Xh %Xh %s%n", type,
            //                   type.getModifiers(), Modifier.ABSTRACT,
            //                   Modifier.toString (type.getModifiers()));
            return true;
        }
        else if (type.isInterface())
        {
            // System.out.println ("type is interface " + type);
            return true;
        }
        else if (type.isArray()) //arrays of non-primitive types
        {
            // System.out.println ("type is array " + type);
            Class<?> eltType = type.getComponentType();
            assert eltType != null; // because type is an array
            return !(eltType.isPrimitive());
        }
        else
            return false;
    }

    /**
     * Returns whether or not the specified field is visible from the Class
     * current.  All fields within instrumented classes are considered
     * visible from everywhere (to match dfej behavior)
     */
    public static boolean isFieldVisible (Class<?> current, Field field)
    {
        Class<?> fclass = field.getDeclaringClass();
        int modifiers = field.getModifiers();

        // If the field is within the current class, it is always visible
        if (current.equals (fclass))
            return (true);

        if (!std_visibility)
        {
            // If the field is in any instrumented class it is always visible
            synchronized (Runtime.all_classes)
            {
                for (ClassInfo ci : Runtime.all_classes)
                {
                    // System.out.printf ("comparing %s vs %s%n", ci.class_name,
                    // fclass.getName());
                    if (ci.class_name.equals(fclass.getName()))
                    {
                        return (true);
                    }
                }
            }
        }

        // Otherwise we consider the variable not to be visible, even
        // though it is.  This mimics dfej behavior
        if (!std_visibility)
            return (false);

        // Everything in the same class is visible
        if (current == fclass)
            return true;

        // If the field is in the same package, it's visible if it is
        // not private or protected.
        if (current.getPackage() != null
            && current.getPackage().equals (fclass.getPackage())) {
            if (Modifier.isPrivate (modifiers)
                || Modifier.isProtected (modifiers))
                return (false);
            else
                return (true);
        }

        // The field must be in an unrelated class, it must be marked
        // public to be visible
        return (Modifier.isPublic (modifiers));
    }


    // Appends as auxiliary information:
    // the package name of the declaring class
    private String appendAuxInfo(Field field)
    {
        // int modifiers = field.getModifiers();

        Package p = field.getDeclaringClass().getPackage();
        String pkgName = (p == null ? null : p.getName());

        //System.out.printf("Package name for type  %s is %s%n", type, pkgName);

        StringBuilder ret = new StringBuilder();

        // String staticString = (Modifier.isStatic(modifiers)) ? "true" : "false";
        ret.append(" # ");
        if (pkgName != null) {
            ret.append("declaringClassPackageName=" + pkgName + ", ");
        }

        return ret.toString();
    }

    /**
    *
    * Checks for "derived" Chicory variables: .class, .tostring, and java.util.List implementors
    * and adds appropriate children to this node.
    */
   protected void checkForDerivedVariables(Class<?> type, String theName,
           String offset)
   {
       checkForListDecl(type, theName, offset); // implements java.util.List?

       //Not fully implemented yet, don't call
       //checkForImplicitList(cinfo, type, name, offset, depth);

       checkForRuntimeClass(type, theName, offset); //.class var
       checkForString(type, theName, offset); //.tostring var
   }

   /**
    * Determines if type implements list
    * and prints associated decls, if necessary
    */
   protected void checkForListDecl(Class<?> type, String theName, String offset)
   {
       if (isArray || type.isPrimitive() || type.isArray())
           return;

       // System.out.printf ("checking %s %sto for list implementation = %b%n",
       //                    type, theName, implementsList (type));

       if (implementsList(type)) {
           @SuppressWarnings("unchecked")
           DaikonVariableInfo child = new ListInfo(offset + theName + "[]",
                                              (Class<? extends List<?>>)type);

           addChild(child);

           boolean ignore = child.check_for_dup_names();

           // .getClass() var
           if (!ignore) {
               DaikonVariableInfo childClass
                   = new DaikonClassInfo(offset + theName + "[]" + class_suffix, classClassName + "[]", stringClassName + "[]", offset + theName + "[]", true);

               child.addChild(childClass);
           }
       }
   }

   /**
    * Checks the given type to see if it requires a .class
    * addition to the decls file.
    * If so, it adds the correct child to this node.
    */
   protected void checkForRuntimeClass(Class<?> type, String theName, String offset)
   {
       if (!shouldAddRuntimeClass(type))
           return;

       String postString = ""; //either array braces or an empty string

       if (theName.contains("[]") || offset.contains ("[]"))
           postString = "[]";

       // add daikoninfo type
       DaikonVariableInfo classInfo
           = new DaikonClassInfo(offset + theName + class_suffix,
                                 classClassName + postString,
                                 stringClassName + postString,
                                 offset + theName,
                                 (offset+theName).contains("[]"));

       addChild(classInfo);
   }

   /**
    * Checks the given type to see if it is a string.
    * If so, it adds the correct child to this node.
    */
   private void checkForString(Class<?> type, String theName, String offset)
   {
       if (!type.equals(String.class))
           return;

       String postString = ""; //either array braces or an empty string

       if ((offset + theName).contains("[]"))
           postString = "[]";

       // add DaikonVariableInfo type
       DaikonVariableInfo stringInfo = new StringInfo(offset + theName + ".toString",
                                                      stringClassName + postString,
                                                      stringClassName + postString,
                                                      offset + theName,
               (offset+theName).contains("[]"));

       addChild(stringInfo);

   }

   /**
    * Returns true iff type implements the List interface.
    *
    * @param type
    * @return true iff type implements the List interface
    */
   public static boolean implementsList(Class<?> type)
   {
       // System.out.println(type);
       Class<?>[] interfaces = type.getInterfaces();
       for (Class<?> inter: interfaces)
       {
           // System.out.println("  implements: " + inter.getName());
           if (inter.equals(java.util.List.class))
               return true;
       }
       return false;
   }

   /**
    * Explores the tree one level deeper (see {@link
    * DaikonVariableInfo}).  This method adds child nodes to this node.
    *
    * For example: "recurse" on a hashcode array object to print the
    * actual array of values or recurse on hashcode variable to print
    * its fields.  Also accounts for derived variables (.class,
    * .tostring) and "recurses" on arrays (that is, adds a variable
    * to print out the arrays's elements as opposed to just the
    * hashcode of the array).
    *
    * @param theName The name of the variable currently being examined,
    *             such as "ballCount"
    * @param offset The representation of the variables we have
    *                previously examined.  For examples, offset could
    *                be "this." in which case offset + name would be
    *                "this.ballCount."
    */
   protected void addChildNodes(ClassInfo cinfo, Class<?> type, String theName,
                                String offset, int depthRemaining) {

       if (type.isPrimitive())
           return;
       else if (type.isArray())
       {
           // don't go into more than one dimension of a multi-dimensional array
           if (isArray)
               return;

           Class<?> eltType = type.getComponentType();
           assert eltType != null; // because type is an array
           if (eltType.isPrimitive()) {

               DaikonVariableInfo newChild
                   = new ArrayInfo(offset + theName + "[]", eltType);

               newChild.check_for_dup_names();

               addChild(newChild);
           }
           // multi-dimensional arrays (not currently used)
           else if (eltType.isArray())
           {
               DaikonVariableInfo newChild
                   = new ArrayInfo(offset + theName + "[]", eltType);

               newChild.check_for_dup_names();

               addChild(newChild);

               debug_vars.indent ("Array variable");
               newChild.addChildNodes(cinfo, eltType, "", offset + theName + "[]", depthRemaining);
               debug_vars.exdent();
           }
           // array is 1-dimensional and element type is a regular class
           else
           {
               DaikonVariableInfo newChild
                   = new ArrayInfo(offset + theName + "[]", eltType);

               boolean ignore = newChild.check_for_dup_names();

               addChild(newChild);

               // Print out the class of each element in the array.  For
               // some reason dfej doesn't include this on returned arrays
               // or parameters.
               // The offset will only be equal to ""
               // if we are examining a local variable (parameter).
               if (!ignore) {
                   if (!theName.equals ("return") && !offset.equals (""))
                       newChild.checkForRuntimeClass (type, theName + "[]",
                                                      offset);

                   newChild.checkForString(eltType, theName + "[]", offset);
               }
               newChild.addClassVars(cinfo, false, eltType,
                               offset + theName + "[].", depthRemaining - 1);

           }
       }
       // regular old class type
       else
       {
           debug_vars.log ("**Depth Remaining = %d%n", depthRemaining);

           if (depthRemaining <= 0)
           {
               // don't recurse any more!
               return;
           }
           if (!systemClass (type))
               addClassVars(cinfo, false, type, offset + theName + ".", depthRemaining - 1);
       }
   }

   /**
    * Returns whether or not the fields of the specified class
    * should be included, based on whether the Class type
    * is a system class or not.  Right now, any system classes are
    * excluded, but a better way of determining this is probably
    * necessary
    */
   public static boolean systemClass (Class<?> type)
   {
       String class_name = type.getName();
       // System.out.printf ("type name is %s%n", class_name);
       if (class_name.startsWith ("java.") || class_name.startsWith("javax."))
           return (true);
       else
           return (false);
   }

   /**
    * Returns the declared type name of this variable.  May include
    * auxiliary information (represented as a suffix starting with "#").
    * @see #getTypeNameOnly()
    */
   public String getTypeName()
   {
       assert typeName != null: "Type name cannot be null";

       return typeName;
   }
   
   /**
    * Return the type name without aux information.
    * @see #getTypeName()
    */
   public String getTypeNameOnly() {
       return typeName.replaceFirst (" # .*", "");
   }

   /**
    * Returns the representation type name of this variable.
    */
   public String getRepTypeName()
   {
       assert typeName != null: "Representation type name cannot be null";

       return repTypeName;
   }

    /** Return the rep type name without the constant value **/
    public String getRepTypeNameOnly() {
        return repTypeName.replaceFirst (" = .*", "");
    }

    /**
     * Returns the constant value of the variable.  If the variable is not
     * static and final, or if the constant value is not available in the
     * class file, returns null.
     */
    public /*@Nullable*/ String get_const_val() {
        return const_val;
    }
    
    /**
     * Returns the function args of the variable. If the variable is not a
     * function, or does not have any arguments, returns null.
     */
    public /*@Nullable*/ String get_function_args() {
    	return function_args;
    }

   /**
    * Returns the comparability information for this variable.
    */
   public String getCompareString()
   {
       assert typeName != null: "Coparability info cannot be null";

       return compareInfoString;
   }

   /**
    * Return true iff the DeclWriter should print this node.
    */
   public boolean declShouldPrint()
   {
       return declShouldPrint;
   }

   /**
    * Return true iff the DTraceWriter should print this node.
    */
   public boolean dTraceShouldPrint()
   {
       return dtraceShouldPrint;
   }

   public boolean dTraceShouldPrintChildren()
   {
       return dtraceShouldPrintChildren;
   }

    /**
     * Compares based on the name of the variable
     */
    public int compareTo (DaikonVariableInfo dv)
    {
        return name.compareTo (dv.name);
    }

    /** Returns whether or not this variable is an array **/
    public boolean isArray()
    {
        return isArray;
    }

    /** Returns the direct child that is an array, null if one does not exist **/
    public /*@Nullable*/ DaikonVariableInfo array_child() {
        for (DaikonVariableInfo dv : children) {
            if (dv.isArray())
                return dv;
        }
        return null;
    }


    /** Returns whether or not this variable has a rep type of hashcode **/
    public boolean isHashcode()
    {
        return getRepTypeName().equals ("hashcode");
    }

    public boolean isHashcodeArray()
    {
        return getRepTypeName().equals ("hashcode[]");
    }

    /** Returns whether or not the declared type of this variable is int **/
    public boolean isInt()
    {
        String[] sarr = getTypeName().split("  *");
        return sarr[0].equals("int");
    }

    /** Returns the kind of the variable (array, field, function, etc) **/
    public abstract VarKind get_var_kind();

    /**
     * Returns the name of this variable relative to its enclosing variable.
     * For example the relative name for 'this.a' is 'a'.
     */
    public /*@Nullable*/ String get_relative_name() {
        return null;
    }

    /** Empty set of variable flags **/
    private static EnumSet<VarFlags> empty_var_flags
        = EnumSet.noneOf (VarFlags.class);

    /**
     * Returns the variable flags for this variable.  Subclasses should call
     * super() and or in any flags that they add
     */
    public EnumSet<VarFlags> get_var_flags() {
        return empty_var_flags;
    }

    /**
     * Returns true iff the variable is static.  Overridden by subclasses that can
     * be static
     */
    public boolean isStatic() {
        return false;
    }

    /**
     * If the variable name has been seen before (which can happen with statics
     * and children of statics, set the flags so that the variable is not considered
     * for decl or dtrace and return true.  Otherwise, do nothing and return false
     */
    private boolean check_for_dup_names () {

        if (ppt_statics.contains (name)) {
            debug_vars.log ("ignoring already included variable %s [%s]",
                            name, getClass());
            if (false && !isStatic())
                System.out.printf ("ignoring already included variable %s [%s]\n",
                                   name, getClass());
            declShouldPrint = false;
            dtraceShouldPrint = false;
            return true;
        } else { // new variable
            ppt_statics.add (name);
            return false;
        }
    }
}
