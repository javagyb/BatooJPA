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
package org.batoo.jpa.parser.impl.annotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.ManyToMany;

import org.batoo.jpa.common.reflect.ReflectHelper;
import org.batoo.jpa.parser.impl.AnnotationParser;
import org.batoo.jpa.parser.impl.metadata.attribute.ManyToManyAttributeMetadataImpl;
import org.batoo.jpa.parser.impl.metadata.type.AttributesMetadataImpl;
import org.batoo.jpa.parser.metadata.ManyToManyAttributeMetadata;

import com.google.common.collect.Sets;

/**
 * PersistenceParser for many-to-many associations.
 * 
 * @author hceylan
 * @since $version
 */
public class ManyToManiesParser extends AbstractAttributesParser<ManyToManyAttributeMetadata> {

	/**
	 * @param parent
	 *            the attributes parser
	 * @param memberMap
	 *            the map of members
	 * @param ormAttributes
	 *            the optional ORM Metadata
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public ManyToManiesParser(AttributesMetadataImpl parent, Map<String, Member> memberMap, List<ManyToManyAttributeMetadata> ormAttributes) {
		super(parent, memberMap, ormAttributes, ManyToMany.class);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void parseAttribute(String name, Member member, ManyToManyAttributeMetadata ormMetadata) {
		if (ormMetadata != null) {
			this.getAttributes().add(new ManyToManyAttributeMetadataImpl(member, ormMetadata));

			ReflectHelper.warnAnnotations(AnnotationParser.LOG, member, Collections.<Class<? extends Annotation>> emptySet());
		}
		else {
			final ManyToMany manyToMany = ReflectHelper.getAnnotation(member, ManyToMany.class);

			final Set<Class<? extends Annotation>> parsed = Sets.<Class<? extends Annotation>> newHashSet(ManyToMany.class);

			final JoinsParser joinsParser = new JoinsParser(member, parsed);

			ReflectHelper.warnAnnotations(AnnotationParser.LOG, member, parsed);

			this.getAttributes().add(
				new ManyToManyAttributeMetadataImpl(member, name, manyToMany, joinsParser.getJoinColumns(), joinsParser.getJoinTable()));
		}
	}
}
