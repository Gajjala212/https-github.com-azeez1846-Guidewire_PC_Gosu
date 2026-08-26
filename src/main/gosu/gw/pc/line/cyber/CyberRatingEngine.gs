package gw.pc.line.cyber

import com.guidewire.pc.model.PolicyPeriod
import java.math.BigDecimal
import java.math.RoundingMode

class CyberRatingEngine {

  public static function rateCyberPolicy(period : PolicyPeriod, aggregateLimit : BigDecimal, mfaEnforced : boolean, backupsDaily : boolean, edrActive : boolean) : BigDecimal {
    print("[GW-LOG] → CyberRatingEngine.rateCyberPolicy")
    if (period == null) return BigDecimal.ZERO

    var limit = aggregateLimit != null ? aggregateLimit : new BigDecimal("1000000.00")
    
    // Base rate: $0.35 per $1,000 limit
    var unitsOf1k = limit.divide(new BigDecimal("1000.00"), 4, RoundingMode.HALF_UP)
    var basePrem = unitsOf1k.multiply(new BigDecimal("3.50")).setScale(2, RoundingMode.HALF_UP)

    var modifier = 1.00
    if (!mfaEnforced) {
      modifier = modifier + 0.35
    } else if (backupsDaily and edrActive) {
      modifier = modifier - 0.15
    }

    var finalPrem = basePrem.multiply(new BigDecimal(modifier)).setScale(2, RoundingMode.HALF_UP)
    return finalPrem
  }
}
