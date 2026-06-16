package com.giabao.linkflow.analytics.mapper;

import com.giabao.linkflow.analytics.dto.response.ClickEventResponse;
import com.giabao.linkflow.analytics.entity.ClickEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClickEventMapper {

    ClickEventResponse toResponse(ClickEvent clickEvent);
}
