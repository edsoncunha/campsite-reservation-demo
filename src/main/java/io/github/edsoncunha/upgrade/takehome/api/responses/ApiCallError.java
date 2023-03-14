package io.github.edsoncunha.upgrade.takehome.api.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ApiCallError<T> {
    String message;
    List<T> details = new ArrayList<>();
}
