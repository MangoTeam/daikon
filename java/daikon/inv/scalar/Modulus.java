package daikon.inv.scalar;

import daikon.*;
import daikon.inv.*;
import utilMDE.*;

class Modulus extends SingleScalar {

  boolean no_values_seen = true;

  int modulus = 0;
  int remainder = 0;

  // an arbitrarily-chosen value used for computing the differences among
  // all the values.
  int value1 = 0;

  Modulus(PptSlice ppt_) {
    super(ppt_);
  }

//   Modulus(Ppt ppt_, VarInfo var_info_) {
//     super(ppt_, var_info_);
//   }

  public String repr() {
    double probability = getProbability();
    return "Modulus(" + var().name + "): "
      + "m=" + modulus + ",r=" + remainder
      + "; probability = " + probability;
  }

  public String format() {
    if (justified())
      return var().name + " = " + remainder + "  (mod " + modulus + ")";
    else
      return null;
  }

  public void add_modified(int value, int count) {
    if (modulus == 1) {
      // We already know this probability fails
      return;
    } else if (no_values_seen) {
      value1 = value;
      return;
    } else if (value == value1) {
      // no new information, so nothing to do
      return;
    } else if (modulus == 0) {
      // only one value seen so far
      modulus = value1 - value;
      remainder = MathMDE.mod_positive(value, modulus);
    } else {
      int new_modulus = MathMDE.gcd(modulus, value1 - value);
      if (new_modulus != modulus) {
	remainder = remainder % new_modulus;
	modulus = new_modulus;
      }
      probability_cache_accurate = false;
    }
    if (modulus == 1) {
      probability_cache = Invariant.PROBABILITY_NEVER;
      probability_cache_accurate = true;
    }
  }

  protected double computeProbability() {
    if (modulus == 1)
      return Invariant.PROBABILITY_NEVER;
    if (modulus == 0)
      return Invariant.PROBABILITY_UNKNOWN;
    double probability_one_elt_modulus = 1 - 1.0/modulus;
    return Math.pow(probability_one_elt_modulus, ppt.num_mod_non_missing_samples());
  }
}
