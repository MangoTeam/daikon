package daikon.inv.binary;

import static jdk.nashorn.internal.objects.Global.print;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.twoScalar.LinearBinaryCore;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.dataflow.qual.SideEffectFree;
import typequals.prototype.qual.NonPrototype;
import typequals.prototype.qual.Prototype;

// @TODO:
//	- address embedded tododos

/** Represents an invariant of - between two long scalars. Prints as {@code x = y + a}. */
public final class PositionSpacing extends BinaryInvariant implements EqualityComparison {

  public LinearBinaryCore core;

  /**
   * Returns whether or not the invariant is valid over the basic types in vis. This only checks
   * basic types (scalar, string, array, etc) and should match the basic superclasses of invariant
   * (SingleFloat, SingleScalarSequence, ThreeScalar, etc). More complex checks that depend on
   * variable details can be implemented in instantiate_ok().
   *
   * @see #instantiate_ok(VarInfo[])
   */
  /** Returns whether or not the specified types are valid. */
  // @TODO
  @Override
  public final boolean valid_types(VarInfo[] vis) {
    print(
        " POSITION SPACING "
            + vis[0].file_rep_type
            + "<- File rep type "
            + vis[0].comparability
            + "<- comparability ");
    if (vis.length != 2) {
      return false;
    }

    System.out.println("\n\nPosition Spacing: " + vis[0].type + "\n\n");
    boolean types_ok =  vis[0].type.toString().startsWith("position-spacing")
                     && vis[1].type.toString().startsWith("position-spacing");
    boolean dim_ok = !vis[0].file_rep_type.isArray() && !vis[1].file_rep_type.isArray();

    return (types_ok && dim_ok && vis[0].file_rep_type.baseIsScalar() && vis[1].file_rep_type.baseIsScalar());
  }

  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 1552769852L;

  // @TODO
  @Override
  protected double computeConfidence() {
    return 0;
  }

  // copied from LinearBinary constructor
  protected @Prototype PositionSpacing() {
    super();
    core = new LinearBinaryCore(this);
  }

  // @TODO
  @Override
  protected Invariant resurrect_done(int[] permutation) {
    return null;
  }

  // @TODO
  @Override
  public InvariantStatus check(
      @Interned Object val1, @Interned Object val2, int mod_index, int count) {
    return null;
  }

  // @TODO
  @Override
  public InvariantStatus add(
      @Interned Object val1, @Interned Object val2, int mod_index, int count) {
    return null;
  }

  private static @Prototype PositionSpacing proto = new @Prototype PositionSpacing();

  // @TODO
  /** Returns the prototype invariant for PositionSpacing */
  public static @Prototype PositionSpacing get_proto() {
    if (proto == null) proto = new PositionSpacing();
    return (proto);
  }

  // @TODO
  @SideEffectFree
  @Override
  public PositionSpacing clone(@GuardSatisfied PositionSpacing this) {
    PositionSpacing result = (PositionSpacing) super.clone();
    result.core = core.clone();
    result.core.wrapper = result;
    return result;
  }

  // @TODO
  @SideEffectFree
  @Override
  public String format_using(@GuardSatisfied PositionSpacing this, OutputFormat format) {
    // @TODO: update to actually represent PositionSpacing constraint i.e. x = y + a
    // return core.format_using(format, var1().name_using(format), var2().name_using(format));
    return var1().name()
        + " POSITION SPACING "
        + var1().file_rep_type
        + "<- File rep type "
        + var2().comparability
        + "<- comparability ";
  }

  // @TODO
  @Override
  public boolean isSameFormula(Invariant other) {
    return false;
  }

  // @TODO
  @Override
  protected @NonPrototype Invariant instantiate_dyn(PptSlice slice) {
    return null;
  }

  // @TODO
  @Override
  public boolean enabled() {
    return false;
  }

  // @TODO
  public InvariantStatus add_modified(long x, long y, int count) {
    return core.add_modified(x, y, count);
  }

  public InvariantStatus check_modified(long x, long y, int count) {
    // @TODO: implement a check that actually makes sense. TBH, since our "variables" are constants,
    //    this could probably be return NO_CHANGE all the time, but think about it harder first.
    return clone().add_modified(x, y, count);
  }

  // @TODO
  @Override
  public double eq_confidence() {
    return 0;
  }

  // @TODO
  @Override
  public VarInfo var1() {
    return null;
  }

  // @TODO
  @Override
  public VarInfo var2() {
    return null;
  }

  // do we want a var3?
}
