package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class SecondAnalogCost extends AnalogCost {
    public SecondAnalogCost(Integer cost, String productType) {
        super(cost, productType);
    }

    public static SecondAnalogCostBuilder builder() {
        return new SecondAnalogCostBuilder();
    }

    public static class SecondAnalogCostBuilder {
        private Integer cost;
        private String productType;

        public SecondAnalogCostBuilder cost(Integer cost) {
            this.cost = cost;
            return this;
        }

        public SecondAnalogCostBuilder productType(String productType) {
            this.productType = productType;
            return this;
        }

        public SecondAnalogCost build() {
            return new SecondAnalogCost(cost, productType);
        }
    }
}
