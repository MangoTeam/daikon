package daikon.inv.binary;

import daikon.*;
import daikon.derive.unary.*;
import daikon.inv.*;
import daikon.inv.binary.twoScalar.LinearBinaryCore;
import java.util.*;
import java.util.logging.Logger;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
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

  public LinearBinaryCore core;

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
  public final boolean valid_types(VarInfo[] vis) {

    if (vis.length != 2) {
      return false;
    }

    boolean dim_ok = !vis[0].file_rep_type.isArray() && !vis[1].file_rep_type.isArray();

    return (dim_ok && vis[0].file_rep_type.baseIsScalar() && vis[1].file_rep_type.baseIsScalar());
  }

  @Override
  protected Invariant resurrect_done(int[] permutation) {
    return this;
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
    core = new LinearBinaryCore(this);
  }

  protected @Prototype PositionSpacing() {
    super();
    core = new LinearBinaryCore(this);
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
  @Override
  public boolean instantiate_ok(VarInfo[] vis) {
    // @TODO: check dec types s.t. they are valid for PositionSpacing constraints
    //	- position attributes.... go get the formal defintiion
    if (!valid_types(vis)) {
      return false;
    }

    return true;
  }

  /** Instantiate an invariant on the specified slice. */
  @Override
  protected PositionSpacing instantiate_dyn(@Prototype PositionSpacing this, PptSlice slice) {

    return new PositionSpacing(slice);
  }

  @SideEffectFree
  @Override
  public PositionSpacing clone(@GuardSatisfied PositionSpacing this) {
    PositionSpacing result = (PositionSpacing) super.clone();
    result.core = core.clone();
    result.core.wrapper = result;
    return result;
  }

  @Pure
  public boolean is_equality_inv() {
    // @TODO: look up what equality_inv is. I suspect that the answer is yes since there's an equals
    // sign
    return true;
  }

  // removed @Override annotation
  protected Invariant resurrect_done_swapped() {
    core.swap();
    return this;
  }

  // perhaps this is not necessary and we don't need to implement EqualityComparison
  // only ever referenced in Invariant through EqualityComparison
  @Override
  public double eq_confidence() {
    return 0;
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

  @Override
  public String repr(@GuardSatisfied PositionSpacing this) {
    // @TODO: figure out where this is printed and rewrite to convey x = y + a
    return "PositionSpacing" + varNames() + ": falsified=" + falsified + "; " + core.repr();
  }

  @SideEffectFree
  @Override
  public String format_using(@GuardSatisfied PositionSpacing this, OutputFormat format) {
    // @TODO: update to actually represent PositionSpacing constraint i.e. x = y + a
    return core.format_using(format, var1().name_using(format), var2().name_using(format));
  }

  @Pure
  @Override
  public boolean isActive() {
    return core.isActive();
  }

  @Override
  public boolean mergeFormulasOk() {
    return (core.mergeFormulasOk());
  }

  /**
   * Merge the invariants in invs to form a new invariant. Each must be a PositionSpacing invariant.
   * The work is done by the LinearBinary core (for now lets hope that works!)
   *
   * @param invs list of invariants to merge. They should all be permuted to match the variable
   *     order in parent_ppt.
   * @param parent_ppt slice that will contain the new invariant
   */
  @Override
  public @Nullable Invariant merge(List<Invariant> invs, PptSlice parent_ppt) {
    // Create a matching list of cores
    List<LinearBinaryCore> cores = new ArrayList<LinearBinaryCore>();
    for (Invariant inv : invs) {
      cores.add(((PositionSpacing) inv).core);
    }

    // Merge the cores and build a new invariant containing the merged core
    PositionSpacing result = new PositionSpacing(parent_ppt);
    LinearBinaryCore newcore = core.merge(cores, result);
    if (newcore == null) {
      return null;
    }
    result.core = newcore;
    return result;
  }

  public InvariantStatus check_modified(long x, long y, int count) {
    // @TODO: implement a check that actually makes sense. TBH, since our "variables" are constants,
    //    this could probably be return NO_CHANGE all the time, but think about it harder first.
    return clone().add_modified(x, y, count);
  }

  public InvariantStatus add_modified(long x, long y, int count) {
    return core.add_modified(x, y, count);
  }

  @Override
  public boolean enoughSamples(@GuardSatisfied PositionSpacing this) {
    return core.enoughSamples();
  }

  @Override
  protected double computeConfidence() {
    return core.computeConfidence();
  }

  @Pure
  @Override
  public boolean isExact() {
    return true;
  }

  // think this should look more like the commented out add() below
  @Override
  public InvariantStatus add(
      @Interned Object val1, @Interned Object val2, int mod_index, int count) {
    // @TODO: check out logging for Invariants with computed constants to see how they're logged
    // Tests for whether a value is missing should be performed before
    // making this call, so as to reduce overall work.
    /*assert !falsified;
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
    }*/
    // for now until we figure out what to do here...
    return InvariantStatus.NO_CHANGE;
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

  public InvariantStatus add_unmodified(long v1, long v2, int count) {
    return InvariantStatus.NO_CHANGE;
  }

  @Pure
  @Override
  public @Nullable DiscardInfo isObviousStatically(VarInfo[] vis) {
    // Obvious derived
    VarInfo var1 = vis[0];
    VarInfo var2 = vis[1];
    // avoid comparing "size(a)" to "size(a)-1"; yields "size(a)-1 = size(a) - 1"
    if (var1.isDerived()
        && (var1.derived instanceof SequenceLength)
        && var2.isDerived()
        && (var2.derived instanceof SequenceLength)) {
      @NonNull SequenceLength sl1 = (SequenceLength) var1.derived;
      @NonNull SequenceLength sl2 = (SequenceLength) var2.derived;
      if (sl1.base == sl2.base) {
        String discardString =
            var1.name()
                + " and "
                + var2.name()
                + " derived from "
                + "same sequence: "
                + sl1.base.name();
        return new DiscardInfo(this, DiscardCode.obvious, discardString);
      }
    }
    // avoid comparing "size(a)-1" to anything; should compare "size(a)" instead
    if (var1.isDerived()
        && (var1.derived instanceof SequenceLength)
        && ((SequenceLength) var1.derived).shift != 0) {
      String discardString =
          "Variables of the form 'size(a)-1' are not compared since 'size(a)' "
              + "will be compared";
      return new DiscardInfo(this, DiscardCode.obvious, discardString);
    }
    if (var2.isDerived()
        && (var2.derived instanceof SequenceLength)
        && ((SequenceLength) var2.derived).shift != 0) {
      String discardString =
          "Variables of the form 'size(a)-1' are not compared since 'size(a)' "
              + "will be compared";
      return new DiscardInfo(this, DiscardCode.obvious, discardString);
    }

    return super.isObviousStatically(vis);
  }

  @Pure
  @Override
  public @Nullable DiscardInfo isObviousDynamically(VarInfo[] vis) {
    DiscardInfo super_result = super.isObviousDynamically(vis);
    if (super_result != null) {
      return super_result;
    }

    if (core.a == 0) {
      return new DiscardInfo(this, DiscardCode.obvious, var2().name() + " is constant");
    }
    if (core.b == 0) {
      return new DiscardInfo(this, DiscardCode.obvious, var1().name() + " is constant");
    }
    //  if (core.a == 1 && core.b == 0) {
    //      return new DiscardInfo(this, DiscardCode.obvious, "Variables are equal");
    //  }
    if (core.a == -core.b && core.c == 0) {
      return new DiscardInfo(this, DiscardCode.obvious, "Variables are equal");
    }
    return null;
  }

  @Pure
  @Override
  public boolean isSameFormula(Invariant other) {
    return core.isSameFormula(((PositionSpacing) other).core);
  }

  @Pure
  @Override
  public boolean isExclusiveFormula(Invariant other) {
    if (other instanceof PositionSpacing) {
      return core.isExclusiveFormula(((PositionSpacing) other).core);
    }
    return false;
  }

  // do we even need find()?
  // Look up a previously instantiated invariant.
  // does this need the suppressing stuff like the commented out find() below?
  public static @Nullable PositionSpacing find(PptSlice ppt) {
    assert ppt.arity() == 2;
    for (Invariant inv : ppt.invs) {
      if (inv instanceof PositionSpacing) {
        return (PositionSpacing) inv;
      }
    }
    return null;
  }

  /*
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
  }*/

  // Returns a vector of PositionSpacing objects.
  // This ought to produce an iterator instead.
  public static List<PositionSpacing> findAll(VarInfo vi) {
    List<PositionSpacing> result = new ArrayList<PositionSpacing>();
    for (PptSlice view : vi.ppt.views_iterable()) {
      if ((view.arity() == 2) && view.usesVar(vi)) {
        PositionSpacing lb = PositionSpacing.find(view);
        if (lb != null) {
          result.add(lb);
        }
      }
    }
    return result;
  }
}
