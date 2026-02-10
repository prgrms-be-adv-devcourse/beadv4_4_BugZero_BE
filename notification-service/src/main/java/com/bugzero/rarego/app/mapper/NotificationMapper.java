package com.bugzero.rarego.app.mapper;

import java.util.List;

import com.bugzero.rarego.domain.Notification;

public interface NotificationMapper<T> {
	boolean supports(Object event);

	List<Notification> map(T event);
}
