package gw.pc.line.marine

import com.guidewire.pc.model.PolicyPeriod
import java.math.BigDecimal
import java.math.RoundingMode

class OceanMarineEngine {

  public static function calculateInsuredCargoValue(invoiceCif : BigDecimal) : BigDecimal {
    print("[GW-LOG] → OceanMarineEngine.calculateInsuredCargoValue")
    if (invoiceCif == null) return BigDecimal.ZERO
    return invoiceCif.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP)
  }

  public static function rateMarineVoyage(period : PolicyPeriod, insuredValue : BigDecimal, isAllRisk : boolean, warStrikes : boolean) : BigDecimal {
    print("[GW-LOG] → OceanMarineEngine.rateMarineVoyage")
    if (insuredValue == null or insuredValue.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO

    var rate = isAllRisk ? new BigDecimal("0.0045") : new BigDecimal("0.0025")
    if (warStrikes) {
      rate = rate.add(new BigDecimal("0.0012"))
    }
    return insuredValue.multiply(rate).setScale(2, RoundingMode.HALF_UP)
  }
}
