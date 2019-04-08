package daikon.inv.unary.scalar;

import daikon.PptSlice;
import daikon.inv.Invariant;
import daikon.inv.InvariantStatus;
import daikon.inv.OutputFormat;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import typequals.prototype.qual.Prototype;

// This invariant is true if the variable is always negative (less than 0).
// This invariant is provided for pedagogical reasons only.

/**
 * Represents the invariant {@code x < 0} where {@code x} is a long scalar. This exists only as an
 * example for the purposes of the manual.
 */
public class Negative extends SingleScalar {
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 11031928L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /** Boolean. True iff Negative invariants should be considered. */
  public static boolean dkconfig_enabled = Invariant.invariantEnabledDefault;

  ///
  /// Required methods
  ///

  private Negative(PptSlice ppt) {
    super(ppt);
  }

  private @Prototype Negative() {
    super();
  }

  private static @Prototype Negative proto = new @Prototype Negative();

  /** Returns the prototype invariant. */
  public static @Prototype Negative get_proto() {
    return proto;
  }

  /** returns whether or not this invariant is enabled */
  @Override
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** instantiate an invariant on the specified slice */
  @Override
  public Negative instantiate_dyn(@Prototype Negative this, PptSlice slice) {
    return new Negative(slice);
  }

  // A printed representation for user output
  @SideEffectFree
  @Override
  public String format_using(@GuardSatisfied Negative this, OutputFormat format) {
    return var().name() + " < 0 NEGAVERSE";
  }

  @Override
  public InvariantStatus check_modified(long v, int count) {
    System.out.println("in check_modified of Negative.java");
    if (v >= 0) {
      return InvariantStatus.FALSIFIED;
    }
    return InvariantStatus.NO_CHANGE;
  }

  @Override
  public InvariantStatus add_modified(long v, int count) {
    return check_modified(v, count);
  }

  @Override
  protected double computeConfidence() {
    // Assume that every variable has a .5 chance of being negative by
    // chance.  Then a set of n values have a have (.5)^n chance of all
    // being negative by chance.
    return 1 - Math.pow(.5, ppt.num_samples());
  }

  @Pure
  @Override
  public boolean isSameFormula(Invariant other) {
    assert other instanceof Negative;
    return true;
  }
}
