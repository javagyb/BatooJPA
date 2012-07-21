/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.batoo.jpa.core.impl.model.type;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.SingularAttribute;

import org.batoo.jpa.common.reflect.ReflectHelper;
import org.batoo.jpa.core.impl.model.MetamodelImpl;
import org.batoo.jpa.core.impl.model.attribute.AssociatedSingularAttribute;
import org.batoo.jpa.core.impl.model.attribute.SingularAttributeImpl;
import org.batoo.jpa.parser.MappingException;
import org.batoo.jpa.parser.metadata.type.EmbeddableMetadata;

import sun.reflect.ConstructorAccessor;

import com.google.common.collect.Lists;

/**
 * Implementation of {@link EmbeddableType}.
 * 
 * @param <X>
 *            The represented embeddable type
 * 
 * @author hceylan
 * @since $version
 */
@SuppressWarnings("restriction")
public class EmbeddableTypeImpl<X> extends ManagedTypeImpl<X> implements EmbeddableType<X> {
	private static final Object[] EMPTY_PARAMS = new Object[] {};

	private ConstructorAccessor constructor;
	private Integer attributeCount;
	private SingularAttributeImpl<?, ?>[] singularMappings;

	/**
	 * @param metamodel
	 *            the metamodel
	 * @param javaType
	 *            the java type
	 * @param metadata
	 *            the metadata
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public EmbeddableTypeImpl(MetamodelImpl metamodel, Class<X> javaType, EmbeddableMetadata metadata) {
		super(metamodel, javaType, metadata);

		this.addAttributes(metadata);

		try {
			this.constructor = ReflectHelper.createConstructor(javaType.getConstructor());
		}
		catch (final Exception e) {
			throw new MappingException("Embeddable type does not have a default constructor", this.getLocator());
		}
	}

	/**
	 * Returns the attribute count of the embeddable.
	 * 
	 * @return the attribute count of the embeddable
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public int getAttributeCount() {
		if (this.attributeCount != null) {
			return this.attributeCount;
		}
		synchronized (this) {
			if (this.attributeCount != null) {
				return this.attributeCount;
			}

			int attributeCount = 0;
			for (final SingularAttribute<?, ?> attribute : this.getSingularAttributes()) {
				switch (attribute.getPersistentAttributeType()) {
					case BASIC:
						attributeCount++;
						break;
					case EMBEDDED:
						attributeCount += ((EmbeddableTypeImpl<?>) attribute).getAttributeCount();
					case MANY_TO_ONE:
					case ONE_TO_ONE:
						attributeCount += ((EntityTypeImpl<?>) attribute.getType()).getPrimaryTable().getPkColumns().size();
				}
			}

			return this.attributeCount = attributeCount;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	/**
	 * Returns the sorted singular mappings of the embeddable.
	 * 
	 * @return the list of sorted singular attributes
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public SingularAttributeImpl<?, ?>[] getSingularMappings() {
		if (this.singularMappings != null) {
			return this.singularMappings;
		}

		synchronized (this) {
			if (this.singularMappings != null) {
				return this.singularMappings;
			}

			final List<SingularAttributeImpl<? super X, ?>> singularMappings = Lists.newArrayList();
			for (final SingularAttribute<? super X, ?> attribute : this.getSingularAttributes()) {
				switch (attribute.getPersistentAttributeType()) {
					case BASIC:
					case EMBEDDED:
						singularMappings.add((SingularAttributeImpl<? super X, ?>) attribute);
						break;
					case MANY_TO_ONE:
					case ONE_TO_ONE:
						final AssociatedSingularAttribute<? super X, ?> association = (AssociatedSingularAttribute<? super X, ?>) attribute;
						if (!association.isOwner() || !association.isJoined()) {
							continue;
						}

						singularMappings.add((SingularAttributeImpl<? super X, ?>) attribute);
				}
			}

			Collections.sort(singularMappings, new Comparator<SingularAttributeImpl<? super X, ?>>() {

				@Override
				public int compare(SingularAttributeImpl<? super X, ?> o1, SingularAttributeImpl<? super X, ?> o2) {
					return o1.getAttributeId().compareTo(o2.getAttributeId());
				}
			});

			return this.singularMappings = singularMappings.toArray(new SingularAttributeImpl[singularMappings.size()]);
		}
	}

	/**
	 * Returns a new instance of the type
	 * 
	 * @return a new instance of the type
	 * 
	 * @since $version
	 * @author hceylan
	 */
	@SuppressWarnings("unchecked")
	public X newInstance() {
		try {
			return (X) this.constructor.newInstance(EmbeddableTypeImpl.EMPTY_PARAMS);
		}
		catch (final Exception e) {} // not possible at this stage

		return null;
	}

}
