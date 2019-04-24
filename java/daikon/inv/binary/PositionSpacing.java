package daikon.inv.binary;

import daikon.*;
import daikon.derive.binary.*;
import daikon.derive.unary.*;
import daikon.inv.*;
import daikon.inv.binary.twoSequence.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.unary.string.*;
import daikon.suppress.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import typequals.prototype.qual.Prototype;

// @TODO:
//	- address embedded todos
//	- find an example of an Invariant that computes a constant and figure out how to thread it
//	  through these functions. TwoScalarCore is one way, but maybe we can get away without
//	  introducing a separate core class
//           - could possibly use UpperBound or LowerBound? (also introduces a sep core class...)
//	- get it running
//	- do rest of Auto Layout constraints

/** Represents an invariant of - between two long scalars. Prints as {@code x = y + a}. */
public final class PositionSpacing extends BinaryInvariant implements EqualityComparison {

  public InvariantStatus check_unmodified(long v1, long v2, int count) {
    return InvariantStatus.NO_CHANGE;
  }

  // if true, swap the order of the invariant variables
  protected boolean swap = false;
  /**
   * Returns whether or not the invariant is valid over the basic types in vis. This only checks
   * basic types (scalar, string, array, etc) and should match the basic superclasses of invariant
   * (SingleFloat, SingleScalarSequence, ThreeScalar, etc). More complex checks that depend on
   * variable details can be implemented in instantiate_ok().
   *
   * @see #instantiate_ok(VarInfo[])
   */
  /** Returns whether or not the specified types are valid. */
  @Override
  final public boolean valid_types(VarInfo[] vis) {
  
      if (vis.length != 2) {
        return false;
      }
  
      boolean dim_ok = !vis[0].file_rep_type.isArray() && !vis[1].file_rep_type.isArray();
  
      return (dim_ok && vis[0].file_rep_type.baseIsScalar() && vis[1].file_rep_type.baseIsScalar());
  }

  @Override
  public String format_using(@GuardSatisfied PositionSpacing this, OutputFormat format) {
    return "String";
  }

  @Override
  protected Invariant resurrect_done(int[] permutation) {
    throw new UnsupportedOperationException();
  }

  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 1552769852L;
  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /** Boolean. True iff PositionSpacing invariants should be considered. */
  public static boolean dkconfig_enabled = Invariant.invariantEnabledDefault;

  public static final Logger debug =
      Logger.getLogger("daikon.inv.binary.twoScalar.PositionSpacing");

  protected PositionSpacing(PptSlice ppt) {
    super(ppt);
  }

  protected @Prototype PositionSpacing() {
    super();
  }

  private static @Prototype PositionSpacing proto = new @Prototype PositionSpacing();
  /** Returns the prototype invariant for PositionSpacing */
  public static @Prototype PositionSpacing get_proto() {
    return proto;
  }

  /** Returns whether or not this invariant is enabled. */
  @Override
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** Returns whether or not the specified var types are valid for PositionSpacing */
  /* @Override public boolean instantiate_ok(VarInfo[] vis) {
    // @TODO: check dec types s.t. they are valid for PositionSpacing constraints
    //	- position attributes.... go get the formal defintiion
    if (!valid_types(vis)) {
      return false;
    }

    return true;
  }*/

  /** Instantiate an invariant on the specified slice. */
  @Override
  protected PositionSpacing instantiate_dyn(@Prototype PositionSpacing this, PptSlice slice) {

    return new PositionSpacing(slice);
  }

  @Pure
  public boolean is_equality_inv() {
    // @TODO: look up what equality_inv is. I suspect that the answer is yes since there's an equals
    // sign
    return true;
  }

  // removed @Override annotation
  protected Invariant resurrect_done_swapped() {
    // @TODO: we don't have symmetry, so we do care when things swap.
    // 	find an example of swapping in another Invariant. Also read what swapping and symmetry
    //    means in docs

    // we don't care if things swap; we have symmetry
    return this;
  }
  /**
   * Returns the first variable. This is the only mechanism by which subclasses should access
   * variables.
   */
  public VarInfo var1(@GuardSatisfied PositionSpacing this) {
    if (swap) {
      return ppt.var_infos[1];
    } else {
      return ppt.var_infos[0];
    }
  }

  /**
   * Returns the first variable. This is the only mechanism by which subclasses should access
   * variables.
   */
  public VarInfo var2(@GuardSatisfied PositionSpacing this) {
    if (swap) {
      return ppt.var_infos[0];
    } else {
      return ppt.var_infos[1];
    }
  }

  @Override
  public InvariantStatus check(
      @Interned Object val1, @Interned Object val2, int mod_index, int count) {
    // Tests for whether a value is missing should be performed before
    // making this call, so as to reduce overall work.
    assert !falsified;
    assert (mod_index >= 0) && (mod_index < 4);
    long v1 = (((Long) val1).longValue());
    long v2 = (((Long) val2).longValue());
    if (mod_index == 0) {
      if (swap) {
        return check_unmodified(v2, v1, count);
      } else {
        return check_unmodified(v1, v2, count);
      }
    } else {
      if (swap) {
        return check_modified(v2, v1, count);
      } else {
        return check_modified(v1, v2, count);
      }
    }
  }

  @Pure
  @Override
  public boolean is_symmetric() {
    // @TODO: double check def'n of symmetry
    return false;
  }

  // JHP: this should be removed in favor of checks in PptTopLevel
  // such as is_equal, is_lessequal, etc.
  // Look up a previously instantiated PositionSpacing relationship.
  // Should this implementation be made more efficient?
  public static @Nullable PositionSpacing find(PptSlice ppt) {
    assert ppt.arity() == 2;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof PositionSpacing) {
        return (PositionSpacing) inv;
      }
    }

    // If the invariant is suppressed, create it
    if ((suppressions != null) && suppressions.suppressed(ppt)) {
      PositionSpacing inv = proto.instantiate_dyn(ppt);
      // System.out.printf("%s is suppressed in ppt %s%n", inv.format(), ppt.name());
      return inv;
    }

    return null;
  }

  @Override
  public String repr(@GuardSatisfied PositionSpacing this) {
    // @TODO: figure out where this is printed and rewrite to convey x = y + a
    return "PositionSpacing" + varNames();
  }

  /*@SideEffectFree
  @Override
  public String format_using(@GuardSatisfied PositionSpacing this, OutputFormat format) {
    // @TODO: update to actually represent PositionSpacing constraint i.e. x = y + a
    String var1name = var1().name_using(format);
    String var2name = var2().name_using(format);

    if ((format == OutputFormat.DAIKON) || (format == OutputFormat.ESCJAVA)) {
      String comparator = "==";
      return var1name + " " + comparator + " " + var2name;
    }

    if (format == OutputFormat.CSHARPCONTRACT) {

      String comparator = "==";
      return var1name + " " + comparator + " " + var2name;
    }

    if (format.isJavaFamily()) {

      if ((var1name.indexOf("daikon.Quant.collectObject") != -1)
          || (var2name.indexOf("daikon.Quant.collectObject") != -1)) {
        return "(warning: it is meaningless to compare hashcodes for values obtained through daikon.Quant.collect... methods:"
            + var1name
            + " == "
            + var2name
            + ")";
      }
      return var1name + " == " + var2name;
    }

    if (format == OutputFormat.SIMPLIFY) {

      String comparator = "EQ";

      return "("
          + comparator
          + " "
          + var1().simplifyFixup(var1name)
          + " "
          + var2().simplifyFixup(var2name)
          + ")";
    }

    return format_unimplemented(format);
  }*/

  // @Override
  public InvariantStatus check_modified(long v1, long v2, int count) {
    // @TODO: implement a check that actually makes sense. TBH, since our "variables" are constants,
    //    this could probably be return NO_CHANGE all the time, but think about it harder first.
    if (!((v1 == v2))) {
      return InvariantStatus.FALSIFIED;
    }
    return InvariantStatus.NO_CHANGE;
  }

  // @Override
  public InvariantStatus add_modified(long v1, long v2, int count) {
    if (logDetail() || debug.isLoggable(Level.FINE)) {
      log(
          debug,
          "add_modified (" + v1 + ", " + v2 + ",  ppt.num_values = " + ppt.num_values() + ")");
    }
    if ((logOn() || debug.isLoggable(Level.FINE))
        && check_modified(v1, v2, count) == InvariantStatus.FALSIFIED)
      log(debug, "destroy in add_modified (" + v1 + ", " + v2 + ",  " + count + ")");

    return check_modified(v1, v2, count);
  }

  /** By default, do nothing if the value hasn't been seen yet. Subclasses can override this. */
  public InvariantStatus add_unmodified(long v1, long v2, int count) {
    return InvariantStatus.NO_CHANGE;
  }
  // This is very tricky, because whether two variables are equal should
  // presumably be transitive, but it's not guaranteed to be so when using
  // this method and not dropping out all variables whose values are ever
  // missing.
  @Override
  protected double computeConfidence() {
    // @TODO: return 1.0. At least for initial implementation
    // Should perhaps check number of samples and be unjustified if too few
    // samples.

    // We MUST check if we have seen samples; otherwise we get
    // undesired transitivity with missing values.
    if (ppt.num_samples() == 0) {
      return Invariant.CONFIDENCE_UNJUSTIFIED;
    }

    // It's an equality invariant.  I ought to use the actual ranges somehow.
    // Actually, I can't even use this .5 test because it can make
    // equality non-transitive.
    // return Math.pow(.5, num_values());
    return Invariant.CONFIDENCE_JUSTIFIED;
  }

  @Override
  public boolean enoughSamples(@GuardSatisfied PositionSpacing this) {
    return (ppt.num_samples() > 0);
  }

  // For Comparison interface, which is satisfied only by exact equalities.
  @Override
  public double eq_confidence() {
    if (isExact()) {
      return getConfidence();
    } else {
      return Invariant.CONFIDENCE_NEVER;
    }
  }

  @Pure
  @Override
  public boolean isExact() {
    return true;
  }

  // // Temporary, for debugging
  // public void destroy() {
  //   if (debug.isLoggable(Level.FINE)) {
  //     System.out.println("PositionSpacing.destroy(" + ppt.name() + ")");
  //     System.out.println(repr());
  //     (new Error()).printStackTrace();
  //   }
  //   super.destroy();
  // }

  @Override
  public InvariantStatus add(
      @Interned Object val1, @Interned Object val2, int mod_index, int count) {
    // Tests for whether a value is missing should be performed before
    // making this call, so as to reduce overall work.
    assert !falsified;
    assert (mod_index >= 0) && (mod_index < 4);
    long v1 = (((Long) val1).longValue());
    long v2 = (((Long) val2).longValue());
    if (mod_index == 0) {
      if (swap) {
        return add_unmodified(v2, v1, count);
      } else {
        return add_unmodified(v1, v2, count);
      }
    } else {
      if (swap) {
        return add_modified(v2, v1, count);
      } else {
        return add_modified(v1, v2, count);
      }
    }
  }
  /* @Override
  public InvariantStatus add(@Interned Object v1, @Interned Object v2, int mod_index, int count) {
    // @TODO: check out logging for Invariants with computed constants to see how they're logged
    if (debug.isLoggable(Level.FINE)) {
      debug.fine(
          "PositionSpacing"
              + ppt.varNames()
              + ".add("
              + v1
              + ","
              + v2
              + ", mod_index="
              + mod_index
              + "), count="
              + count
              + ")");
    }
    return super.add(v1, v2, mod_index, count);
  }*/

  @Pure
  @Override
  public boolean isSameFormula(Invariant other) {
    return true;
  }

  /*@Pure
  @Override
  public boolean isExclusiveFormula(Invariant other) {

    // Also ought to check against LinearBinary, etc.

    if ((other instanceof IntLessThan)
        || (other instanceof IntGreaterThan)
        || (other instanceof IntNonEqual)) {
      return true;
    }

    return false;
  }*/

  @Override
  public @Nullable DiscardInfo isObviousStatically(VarInfo[] vis) {
    // @TODO: figure out what "Obvious" means in static and dynamic contexts
    final VarInfo var1 = vis[0];
    final VarInfo var2 = vis[1];

    // If A.minvalue==A.maxvalue==B.minvalue==B.maxvalue, then
    // there's nothing to see here.
    if (var1.aux.hasValue(VarInfoAux.MINIMUM_VALUE)
        && var1.aux.hasValue(VarInfoAux.MAXIMUM_VALUE)
        && var2.aux.hasValue(VarInfoAux.MINIMUM_VALUE)
        && var2.aux.hasValue(VarInfoAux.MAXIMUM_VALUE)) {
      @SuppressWarnings("keyfor") // EnsuresKeyFor for multiple maps
      int minA = var1.aux.getInt(VarInfoAux.MINIMUM_VALUE),
          maxA = var1.aux.getInt(VarInfoAux.MAXIMUM_VALUE),
          minB = var2.aux.getInt(VarInfoAux.MINIMUM_VALUE),
          maxB = var2.aux.getInt(VarInfoAux.MAXIMUM_VALUE);

      if (minA == maxA && maxA == minB && minB == maxB) {
        return new DiscardInfo(
            this, DiscardCode.obvious, var1.name() + " == " + var2.name() + " is already known");
      }
    }

    return super.isObviousStatically(vis);
  }

  /**
   * Since this invariant can be a postProcessed equality, we have to handle isObvious especially to
   * avoid circular isObvious relations. We only check if this.ppt.var_infos imply obviousness
   * rather than the cartesian product on the equality set.
   */
  /* @Pure
  @Override
  public @Nullable DiscardInfo isObviousStatically_SomeInEquality() {
    if (var1().equalitySet == var2().equalitySet) {
      return isObviousStatically(this.ppt.var_infos);
    } else {
      return super.isObviousStatically_SomeInEquality();
    }
  }*/

  /**
   * Since this invariant can be a postProcessed equality, we have to handle isObvious especially to
   * avoid circular isObvious relations. We only check if this.ppt.var_infos imply obviousness
   * rather than the cartesian product on the equality set.
   */
  /*@Pure
  @Override
  public @Nullable DiscardInfo isObviousDynamically_SomeInEquality() {
    if (var1().equalitySet == var2().equalitySet) {
      return isObviousDynamically(this.ppt.var_infos);
    } else {
      return super.isObviousDynamically_SomeInEquality();
    }
  }*/

  @Pure
  @Override
  public @Nullable DiscardInfo isObviousDynamically(VarInfo[] vis) {

    // JHP: We might consider adding a check over bounds.   If
    // x < c and y > c then we know that x < y.  Similarly for
    // x > c and y < c.  We could also substitute oneof for
    // one or both of the bound checks.

    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }

    VarInfo var1 = vis[0];
    VarInfo var2 = vis[1];

    // a+c=b+c is implied, because a=b must have also been reported.
    if (var1.is_add() && var2.is_add() && (var1.get_add_amount() == var2.get_add_amount()))
      return new DiscardInfo(
          this,
          DiscardCode.obvious,
          "Invariants of the form a+c==b+c are implied since a==b is reported.");

    DiscardInfo di = null;

    // Check for the same invariant over enclosing arrays
    di = pairwise_implies(vis);
    if (di != null) {
      return di;
    }

    // Check for size(A[]) == Size(B[]) where A[] == B[]
    /*di = array_eq_implies(vis);
    if (di != null) {
      return di;
    }*/

    { // Sequence length tests
      SequenceLength sl1 = null;
      if (var1.isDerived() && (var1.derived instanceof SequenceLength)) {
        sl1 = (SequenceLength) var1.derived;
      }
      SequenceLength sl2 = null;
      if (var2.isDerived() && (var2.derived instanceof SequenceLength)) {
        sl2 = (SequenceLength) var2.derived;
      }

      // "size(a)-1 cmp size(b)-1" is never even instantiated;
      // use "size(a) cmp size(b)" instead.

      // This might never get invoked, as equality is printed out specially.
      VarInfo s1 = (sl1 == null) ? null : sl1.base;
      VarInfo s2 = (sl2 == null) ? null : sl2.base;
      if ((s1 != null) && (s2 != null) && (s1.equalitySet == s2.equalitySet)) {
        // lengths of equal arrays being compared
        String n1 = var1.name();
        String n2 = var2.name();
        return new DiscardInfo(
            this,
            DiscardCode.obvious,
            n1 + " and " + n2 + " are equal arrays, so equal size is implied");
      }
    }

    return null;
  } // isObviousDynamically

  /**
   * If both variables are subscripts and the underlying arrays have the same invariant, then this
   * invariant is implied:
   *
   * <pre>(x[] op y[]) ^ (i == j) &rArr; (x[i] op y[j])</pre>
   */
  private @Nullable DiscardInfo pairwise_implies(VarInfo[] vis) {
    // @TODO: what?

    VarInfo v1 = vis[0];
    VarInfo v2 = vis[1];

    // Make sure v1 and v2 are SequenceScalarSubscript with the same shift
    if (!v1.isDerived() || !(v1.derived instanceof SequenceScalarSubscript)) {
      return null;
    }
    if (!v2.isDerived() || !(v2.derived instanceof SequenceScalarSubscript)) {
      return null;
    }
    @NonNull SequenceScalarSubscript der1 = (SequenceScalarSubscript) v1.derived;
    @NonNull SequenceScalarSubscript der2 = (SequenceScalarSubscript) v2.derived;
    if (der1.index_shift != der2.index_shift) {
      return null;
    }

    // Make sure that the indices are equal
    if (!ppt.parent.is_equal(der1.sclvar().canonicalRep(), der2.sclvar().canonicalRep())) {
      return null;
    }

    // See if the same relationship holds over the arrays
    Invariant proto = PairwiseIntEqual.get_proto();
    DiscardInfo di = ppt.parent.check_implied_canonical(this, der1.seqvar(), der2.seqvar(), proto);
    return di;
  }

  /**
   * If the equality is between two array size variables, check to see if the underlying arrays are
   * equal:
   *
   * <pre>(x[] = y[]) &rArr; size(x[]) = size(y[])</pre>
   */
  /*private @Nullable DiscardInfo array_eq_implies(VarInfo[] vis) {

    // Make sure v1 and v2 are size(array) with the same shift
    VarInfo v1 = vis[0];
    if (!v1.isDerived() || !(v1.derived instanceof SequenceLength)) {
      return null;
    }
    VarInfo v2 = vis[1];
    if (!v2.isDerived() || !(v2.derived instanceof SequenceLength)) {
      return null;
    }
    if (!v1.derived.isSameFormula(v2.derived)) {
      return null;
    }

    VarInfo seqvar1 = v1.derived.getBase(0);
    VarInfo seqvar2 = v2.derived.getBase(0);
    if (ppt.parent.is_equal(seqvar1, seqvar2)) {
      return new DiscardInfo(
          this,
          DiscardCode.obvious,
          "Implied by "
              + seqvar1
              + " == "
              + seqvar2
              + " and "
              + var1()
              + " == "
              + v1
              + " and "
              + var2()
              + " == "
              + v2);
    }

    return null;
  }*/

  /** NI suppressions, initialized in get_ni_suppressions() */
  private static @Nullable NISuppressionSet suppressions = null;

  /** Returns the non-instantiating suppressions for this invariant. */
  @Pure
  @Override
  public @Nullable NISuppressionSet get_ni_suppressions() {
    return null;
  }
}
