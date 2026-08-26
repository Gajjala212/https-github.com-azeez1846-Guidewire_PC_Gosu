package gw.pc.line.surety

import com.guidewire.pc.model.PolicyPeriod
import java.math.BigDecimal
import java.math.RoundingMode

class SuretyBondEngine {

  public static function rateContractBond(period : PolicyPeriod, penalSum : BigDecimal, bondType : String) : BigDecimal {
    print("[GW-LOG] → SuretyBondEngine.rateContractBond")
    if (penalSum == null or penalSum.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO

    if ("BID".equals(bondType)) {
      return new BigDecimal("250.00")
    }

    // Baseline 1.2% rate for general performance bond
    var rate = new BigDecimal("0.012")
    var prem = penalSum.multiply(rate).setScale(2, RoundingMode.HALF_UP)
    return prem
  }

  public static function calculateAvailableCapacity(workingCapital : BigDecimal, netWorth : BigDecimal, activeWip : BigDecimal) : BigDecimal {
    print("[GW-LOG] → SuretyBondEngine.calculateAvailableCapacity")
    var wc = workingCapital != null ? workingCapital : BigDecimal.ZERO
    var nw = netWorth != null ? netWorth : BigDecimal.ZERO
    var wip = activeWip != null ? activeWip : BigDecimal.ZERO

    var aggWc = wc.multiply(new BigDecimal("15"))
    var aggNw = nw.multiply(new BigDecimal("7"))
    var aggCap = aggWc.min(aggNw)

    return aggCap.subtract(wip).setScale(2, RoundingMode.HALF_UP)
  }
}
