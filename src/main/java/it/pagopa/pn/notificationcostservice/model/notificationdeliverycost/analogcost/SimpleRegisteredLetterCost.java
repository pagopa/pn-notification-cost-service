package it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.analogcost;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@NoArgsConstructor
public class SimpleRegisteredLetterCost extends AnalogCost {
    public SimpleRegisteredLetterCost(Integer cost, String productType) {
        super(cost, productType);
    }

    public static SimpleRegisteredLetterCostDtoBuilder builder() {
        return new SimpleRegisteredLetterCostDtoBuilder();
    }

    public static class SimpleRegisteredLetterCostDtoBuilder {
        private Integer cost;
        private String productType;

        public SimpleRegisteredLetterCostDtoBuilder cost(Integer cost) {
            this.cost = cost;
            return this;
        }

        public SimpleRegisteredLetterCostDtoBuilder productType(String productType) {
            this.productType = productType;
            return this;
        }

        public SimpleRegisteredLetterCost build() {
            return new SimpleRegisteredLetterCost(cost, productType);
        }
    }
}
