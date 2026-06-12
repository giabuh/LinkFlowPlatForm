package com.giabao.linkflow.link.mapper;

import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.entity.Link;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LinkMapper {

    LinkResponse toResponse(Link link);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "shortCode", ignore = true)
    @Mapping(target = "clickCount", ignore = true)
    @Mapping(target = "active", ignore = true)
    Link toEntity(CreateLinkRequest request);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "shortCode", ignore = true)
    @Mapping(target = "clickCount", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateLinkRequest request, @MappingTarget Link link);
}
