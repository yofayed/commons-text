/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.text;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * Generates random Unicode strings containing the specified number of code points.
 * Instances are created using a builder class, which allows the
 * callers to define the properties of the generator. See the documentation for the
 * {@link Builder} class to see available properties.
 * </p>
 * 
 * <pre>
 * // Generates a 20 code point string, using only the letters a-z
 * RandomStringGenerator generator = new RandomStringGenerator.Builder().withinRange('a', 'z').build();
 * String random = generator.generate(20);
 * </pre>
 * 
 * <p>
 * {@code RandomStringBuilder} instances are immutable and thread-safe.
 * </p>
 * 
 * @since 1.0
 */
public final class RandomStringGenerator {

    private final int minimumCodePoint;
    private final int maximumCodePoint;
    private final Set<CharacterPredicate> inclusivePredicates;
    private final Random random;


    /**
     * Constructs the generator.
     * 
     * @param minimumCodePoint
     *            smallest allowed code point (inclusive)
     * @param maximumCodePoint
     *            largest allowed code point (inclusive)
     * @param inclusivePredicates
     *            filters for code points
     * @param random
     *            source of randomness
     */
    private RandomStringGenerator(int minimumCodePoint, int maximumCodePoint,
            Set<CharacterPredicate> inclusivePredicates, Random random) {
        this.minimumCodePoint = minimumCodePoint;
        this.maximumCodePoint = maximumCodePoint;
        this.inclusivePredicates = inclusivePredicates;
        this.random = random;
    }
    
    /**
     * Generates a random number within a range, using a
     * {@link ThreadLocalRandom} instance or the user-supplied source of
     * randomness.
     * 
     * @param minInclusive
     *            the minimum value allowed
     * @param maxInclusive
     *            the maximum value allowed
     * @return the random number.
     */
    private int generateRandomNumber(final int minInclusive, final int maxInclusive) {
        if (random != null) {
            return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
        }

        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }


    /**
     * <p>
     * Generates a random string, containing the specified number of code points.
     * </p>
     * <p>Code points are randomly selected between the minimum and maximum values defined
     * in the generator.
     * Surrogate and private use characters are not returned, although the
     * resulting string may contain pairs of surrogates that together encode a
     * supplementary character.
     * </p>
     * <p>
     * Note: the number of {@code char} code units generated will exceed
     * {@code length} if the string contains supplementary characters. See the
     * {@link Character} documentation to understand how Java stores Unicode
     * values.
     * </p>
     * 
     * @param length
     *            the number of code points to generate
     * @return the generated string
     * @throws IllegalArgumentException
     *             if {@code length < 0}
     * @since 1.0
     */
    public String generate(final int length) {
        if (length == 0) {
            return "";
        }
        
        if (length < 0) {
            throw new IllegalArgumentException(String.format("Length %d is smaller than zero.", length));
        }

        final StringBuilder builder = new StringBuilder(length);
        long remaining = length;

        do {
            int codePoint = generateRandomNumber(minimumCodePoint, maximumCodePoint);

            switch (Character.getType(codePoint)) {
            case Character.UNASSIGNED:
            case Character.PRIVATE_USE:
            case Character.SURROGATE:
                continue;
            }

            if (inclusivePredicates != null) {
                boolean matchedFilter = false;
                for (CharacterPredicate predicate : inclusivePredicates) {
                    if (predicate.test(codePoint)) {
                        matchedFilter = true;
                        break;
                    }
                }
                if (!matchedFilter) {
                    continue;
                }
            }

            builder.appendCodePoint(codePoint);
            remaining--;

        } while (remaining != 0);

        return builder.toString();
    }
    
    
    /**
     * <p>A builder for generating {@code RandomStringGenerator} instances.</p>
     * <p>The behaviour of a generator is controlled by properties set by this
     * builder. Each property has a default value, which can be overridden by
     * calling the methods defined in this class, prior to calling {@link #build()}.</p>
     * 
     * <p>All the property setting methods return the {@code Builder} instance to allow for method chaining.</p>
     * 
     * <p>The minimum and maximum code point values are defined using {@link #withinRange(int, int)}. The
     * default values are {@code 0} and {@link Character#MAX_CODE_POINT} respectively.</p>
     * 
     * <p>The source of randomness can be set using {@link #usingRandom(Random)}, otherwise {@link ThreadLocalRandom}
     * is used.</p>
     * 
     * <p>The type of code points returned can be filtered using {@link #filteredBy(CharacterPredicate...)}, 
     * which defines a collection of
     * tests that are applied to the randomly generated code points. The code points
     * will only be included in the result if they pass at least one of the tests.
     * Some commonly used predicates are provided by the {@link CharacterPredicates} enum.</p>
     * 
     * <p>This class is not thread safe.</p>
     * @since 1.0
     */
    public static class Builder implements org.apache.commons.text.Builder<RandomStringGenerator> {
        
        /**
         * The default maximum code point allowed: {@link Character#MAX_CODE_POINT}
         * ({@value})
         * 
         * @since 1.0
         */
        public static final int DEFAULT_MAXIMUM_CODE_POINT = Character.MAX_CODE_POINT;
        
        /**
         * The default string length produced by this builder: {@value}
         * 
         * @since 1.0
         */
        public static final int DEFAULT_LENGTH = 0;

        /**
         * The default minimum code point allowed: {@value}
         * 
         * @since 1.0
         */
        public static final int DEFAULT_MINIMUM_CODE_POINT = 0;

        private int minimumCodePoint = DEFAULT_MINIMUM_CODE_POINT;
        private int maximumCodePoint = DEFAULT_MAXIMUM_CODE_POINT;
        private Set<CharacterPredicate> inclusivePredicates;
        private Random random;
        
        
        /**
         * <p>
         * Specifies the minimum and maximum code points allowed in the generated
         * string.
         * </p>
         * 
         * @param minimumCodePoint
         *            the smallest code point allowed (inclusive)
         * @param maximumCodePoint
         *            the largest code point allowed (inclusive)
         * @return {@code this}, to allow method chaining
         * @throws IllegalArgumentException
         *             if {@code maximumCodePoint >}
         *             {@link Character#MAX_CODE_POINT}
         * @throws IllegalArgumentException
         *             if {@code minimumCodePoint < 0}
         * @throws IllegalArgumentException
         *             if {@code minimumCodePoint > maximumCodePoint}
         * @since 1.0
         */
        public Builder withinRange(final int minimumCodePoint, final int maximumCodePoint) {
            if (minimumCodePoint > maximumCodePoint) {
                throw new IllegalArgumentException(String.format(
                        "Minimum code point %d is larger than maximum code point %d", minimumCodePoint, maximumCodePoint));
            }
            if (minimumCodePoint < 0) {
                throw new IllegalArgumentException(String.format("Minimum code point %d is negative", minimumCodePoint));
            }
            if (maximumCodePoint > Character.MAX_CODE_POINT) {
                throw new IllegalArgumentException(
                        String.format("Value %d is larger than Character.MAX_CODE_POINT.", maximumCodePoint));
            }

            this.minimumCodePoint = minimumCodePoint;
            this.maximumCodePoint = maximumCodePoint;
            return this;
        }
        
        /**
         * <p>
         * Limits the characters in the generated string to those that match at
         * least one of the predicates supplied.
         * </p>
         * 
         * <p>
         * Passing {@code null} or an empty array to this method will revert to the
         * default behaviour of allowing any character. Multiple calls to this
         * method will replace the previously stored predicates.
         * </p>
         * 
         * @param predicates
         *            the predicates, may be {@code null} or empty
         * @return {@code this}, to allow method chaining
         * @since 1.0
         */
        public Builder filteredBy(final CharacterPredicate... predicates) {
            if (predicates == null || predicates.length == 0) {
                inclusivePredicates = null;
                return this;
            }

            if (inclusivePredicates == null) {
                inclusivePredicates = new HashSet<>();
            } else {
                inclusivePredicates.clear();
            }

            for (CharacterPredicate predicate : predicates) {
                inclusivePredicates.add(predicate);
            }

            return this;
        }
        
        /**
         * <p>
         * Overrides the default source of randomness.
         * </p>
         * 
         * <p>
         * Passing {@code null} to this method will revert to the default source of
         * randomness.
         * </p>
         * 
         * @param random
         *            the source of randomness, may be {@code null}
         * @return {@code this}, to allow method chaining
         * @since 1.0
         */
        public Builder usingRandom(final Random random) {
            this.random = random;
            return this;
        }

        /**
         * <p>Builds the {@code RandomStringGenerator} using the properties specified.</p>
         */
        @Override
        public RandomStringGenerator build() {
            return new RandomStringGenerator(minimumCodePoint, maximumCodePoint, inclusivePredicates, random);
        }
        
    }
}
