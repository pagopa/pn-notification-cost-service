package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class FirstAnalogCostDto extends AnalogCostDto {
    public FirstAnalogCostDto(Integer cost, String productType) {
        super(cost, productType);
    }

    public static FirstAnalogCostBuilder builder() {
        return new FirstAnalogCostBuilder();
    }

    public static class FirstAnalogCostBuilder {
        private Integer cost;
        private String productType;

        public FirstAnalogCostBuilder cost(Integer cost) {
            this.cost = cost;
            return this;
        }

        public FirstAnalogCostBuilder productType(String productType) {
            this.productType = productType;
            return this;
        }

        public FirstAnalogCostDto build() {
            return new FirstAnalogCostDto(cost, productType);
        }
    }
}
