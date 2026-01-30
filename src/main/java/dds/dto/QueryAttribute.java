package dds.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QueryAttribute {
    private String attribute;
    private String operator;
    private String value;
}