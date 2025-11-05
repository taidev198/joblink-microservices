package com.joblink.vocabulary.mapper;

import com.joblink.vocabulary.dto.response.WordResponse;
import com.joblink.vocabulary.model.entity.Word;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WordMapper {
    WordMapper INSTANCE = Mappers.getMapper(WordMapper.class);
    
    WordResponse toResponse(Word word);
    
    List<WordResponse> toResponseList(List<Word> words);
}

