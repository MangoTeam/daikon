package daikon.inv.unary.sequence;

import daikon.*;

import utilMDE.*;

import java.util.*;

public final class SingleSequenceFactory {

  // Adds the appropriate new Invariant objects to the specified Invariants
  // collection.
  public static Vector instantiate(PptSlice ppt, int pass) {

    VarInfo var = ppt.var_infos[0];
    Assert.assert(var.rep_type == ProglangType.INT_ARRAY);
    Assert.assert(var.type.pseudoDimensions() > 0);

    Vector result = new Vector();
    if (pass == 1) {
      result.add(OneOfSequence.instantiate(ppt));
      result.add(EltOneOf.instantiate(ppt));
    } else if (pass == 2) {
      EltOneOf eoo = EltOneOf.find(ppt);
      if (!((eoo != null) && (eoo.num_elts() == 1))) {
        result.add(EltNonZero.instantiate(ppt));
        result.add(NoDuplicates.instantiate(ppt));
	result.add(CommonSequence.instantiate(ppt));
        if (var.type.elementIsIntegral()) {
          result.add(EltwiseIntComparison.instantiate(ppt));
          result.add(EltLowerBound.instantiate(ppt));
          result.add(EltUpperBound.instantiate(ppt));
          result.add(SeqIndexComparison.instantiate(ppt));
          result.add(SeqIndexNonEqual.instantiate(ppt));
        }
      }
    }
    return result;
  }

  private SingleSequenceFactory() {
  }

}
