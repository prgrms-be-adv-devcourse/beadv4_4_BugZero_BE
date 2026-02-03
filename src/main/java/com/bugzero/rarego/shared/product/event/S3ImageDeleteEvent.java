package com.bugzero.rarego.shared.product.event;

import java.util.List;

public record S3ImageDeleteEvent(List<String> paths) {
}
