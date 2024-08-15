package com.luluroute.ms.service.util;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ObjectMapperUtil {

	private static final ModelMapper modelMapper;

	static {
		modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE)
				.setSkipNullEnabled(true)
				.setPropertyCondition(Conditions.isNotNull());
	}

	public static <D, T> D map(final T source, Class<D> resultClass) {
		return modelMapper.map(source, resultClass);
	}

	public static <D, T> List<D> mapAll(final Collection<T> sourceList, Class<D> resultClass) {
		return sourceList.stream().map(source -> map(source, resultClass)).collect(Collectors.toList());
	}

	public static <D, T> List<D> mapIterator(final Iterable<T> sourceIterator, Class<D> resultClass) {
		return StreamSupport.stream(sourceIterator.spliterator(), false)
				.map(source -> map(source, resultClass)).collect(Collectors.toList());
	}

	public static <S, D> D map(final S source, D destination) {
        modelMapper.map(source, destination);
        return destination;
    }

	/**
	 * Copies properties from one object to another
	 * @param source
	 * @destination
	 * @return
	 */
	public static void copyNonNullProperties(Object source, Object destination){
		BeanUtils.copyProperties(source, destination,
				getNullPropertyNames(source));
	}

	/**
	 * Returns an array of null properties of an object
	 * @param source
	 * @return
	 */
	private static String[] getNullPropertyNames (Object source) {
		final BeanWrapper src = new BeanWrapperImpl(source);
		java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
		Set<String> emptyNames = new HashSet<>();
		for(java.beans.PropertyDescriptor pd : pds) {
			//check if value of this property is null then add it to the collection
			Object srcValue = src.getPropertyValue(pd.getName());
			if (srcValue == null) emptyNames.add(pd.getName());
		}
		String[] result = new String[emptyNames.size()];
		return (String[]) emptyNames.toArray(result);
	}


}