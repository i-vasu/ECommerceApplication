package com.app.marketing.mappers;

import com.app.marketing.entities.Blog;
import com.app.marketing.payloads.BlogDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BlogMapper {

    BlogDTO blogToBlogDTO(Blog blog);

    Blog blogDTOToBlog(BlogDTO blogDTO);
}
