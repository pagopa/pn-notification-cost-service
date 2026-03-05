package it.pagopa.pn.notificationcostservice.dto.notificationdeliverycost.analogcost;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class SimpleRegisteredLetterCostDto extends AnalogCostDto {
    public SimpleRegisteredLetterCostDto(Integer cost, String productType) {
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

        public SimpleRegisteredLetterCostDto build() {
            return new SimpleRegisteredLetterCostDto(cost, productType);
        }
    }
}
