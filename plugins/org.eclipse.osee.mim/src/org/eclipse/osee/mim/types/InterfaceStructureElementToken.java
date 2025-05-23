/*********************************************************************
 * Copyright (c) 2021 Boeing
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Boeing - initial API and implementation
 **********************************************************************/
package org.eclipse.osee.mim.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.osee.accessor.types.ArtifactAccessorResultWithGammas;
import org.eclipse.osee.accessor.types.AttributePojo;
import org.eclipse.osee.framework.core.data.ApplicabilityId;
import org.eclipse.osee.framework.core.data.ApplicabilityToken;
import org.eclipse.osee.framework.core.data.ArtifactReadable;
import org.eclipse.osee.framework.core.data.ArtifactToken;
import org.eclipse.osee.framework.core.data.AttributeTypeToken;
import org.eclipse.osee.framework.core.data.GammaId;
import org.eclipse.osee.framework.core.enums.CoreArtifactTypes;
import org.eclipse.osee.framework.core.enums.CoreAttributeTypes;
import org.eclipse.osee.framework.core.enums.CoreRelationTypes;
import org.eclipse.osee.framework.jdk.core.type.Id;
import org.eclipse.osee.framework.jdk.core.util.Strings;
import org.eclipse.osee.orcs.rest.model.transaction.Attribute;
import org.eclipse.osee.orcs.rest.model.transaction.CreateArtifact;

/**
 * @author Luciano T. Vaglienti
 */
public class InterfaceStructureElementToken extends ArtifactAccessorResultWithGammas {
   public static final InterfaceStructureElementToken SENTINEL = new InterfaceStructureElementToken();

   private AttributePojo<String> enumLiteral =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementEnumLiteral, GammaId.SENTINEL, "", "");
   private AttributePojo<Boolean> InterfaceElementAlterable =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementAlterable, GammaId.SENTINEL, false, "");
   private AttributePojo<Boolean> interfaceElementArrayHeader =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementArrayHeader, GammaId.SENTINEL, false, "");
   private AttributePojo<Boolean> interfaceElementWriteArrayHeaderName = AttributePojo.valueOf(Id.SENTINEL,
      CoreAttributeTypes.InterfaceElementWriteArrayHeaderName, GammaId.SENTINEL, false, "");
   private AttributePojo<String> interfaceElementArrayIndexOrder =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementArrayIndexOrder, GammaId.SENTINEL,
         ElementArrayIndexOrder.OUTER_INNER.toString(), "");
   private AttributePojo<String> interfaceElementArrayIndexDelimiterOne = AttributePojo.valueOf(Id.SENTINEL,
      CoreAttributeTypes.InterfaceElementArrayIndexDelimiterOne, GammaId.SENTINEL, " ", "");
   private AttributePojo<String> interfaceElementArrayIndexDelimiterTwo = AttributePojo.valueOf(Id.SENTINEL,
      CoreAttributeTypes.InterfaceElementArrayIndexDelimiterTwo, GammaId.SENTINEL, " ", "");
   private AttributePojo<String> Notes =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.Notes, GammaId.SENTINEL, "", "");
   private AttributePojo<String> Description =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.Description, GammaId.SENTINEL, "", "");
   private AttributePojo<Integer> InterfaceElementIndexStart =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementIndexStart, GammaId.SENTINEL, 0, "");
   private AttributePojo<Integer> InterfaceElementIndexEnd =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementIndexEnd, GammaId.SENTINEL, 0, "");
   private List<InterfaceStructureElementToken> arrayElements = new LinkedList<>();
   private InterfaceEnumerationSet arrayDescriptionSet = InterfaceEnumerationSet.SENTINEL;
   private AttributePojo<Boolean> interfaceElementBlockData =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementBlockData, GammaId.SENTINEL, false, "");

   private Double beginByte = 0.0;
   private Double beginWord = 0.0;

   private ApplicabilityToken applicability = ApplicabilityToken.BASE;

   private AttributePojo<String> InterfaceDefaultValue =
      AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceDefaultValue, GammaId.SENTINEL, "", "");
   private boolean autogenerated = false;
   private boolean includedInCounts = true;
   private boolean isArrayChild = false;
   private boolean hasNegativeEndByteOffset = false;
   private PlatformTypeToken platformType = PlatformTypeToken.SENTINEL;
   private boolean shouldValidate = false;
   private int validationSize = 8;
   /**
    * @param art
    */
   public InterfaceStructureElementToken(ArtifactToken art) {
      this((ArtifactReadable) art);
   }

   /**
    * @param art
    */
   public InterfaceStructureElementToken(ArtifactReadable art) {
      super(art);
      this.setId(art.getId());
      this.setName(AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.Name, "")));
      this.setInterfaceElementAlterable(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementAlterable, false)));
      this.setInterfaceElementArrayHeader(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementArrayHeader, false)));
      this.setInterfaceElementWriteArrayHeaderName(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementWriteArrayHeaderName, false)));
      this.setInterfaceElementIndexStart(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementIndexStart, 0)));
      this.setInterfaceElementIndexEnd(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementIndexEnd, 0)));
      this.setNotes(AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.Notes, "")));
      this.setDescription(AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.Description, "")));
      this.setEnumLiteral(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementEnumLiteral, "")));
      this.setInterfaceDefaultValue(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceDefaultValue, "")));
      this.setInterfaceElementArrayIndexOrder(AttributePojo.valueOf(art.getSoleAttribute(
         CoreAttributeTypes.InterfaceElementArrayIndexOrder, ElementArrayIndexOrder.OUTER_INNER.toString())));
      this.setInterfaceElementArrayIndexDelimiterOne(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementArrayIndexDelimiterOne, "")));
      this.setInterfaceElementArrayIndexDelimiterTwo(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementArrayIndexDelimiterTwo, "")));
      this.setInterfaceElementBlockData(
         AttributePojo.valueOf(art.getSoleAttribute(CoreAttributeTypes.InterfaceElementBlockData, false)));
      ArtifactReadable pTypeArt =
         art.getRelated(CoreRelationTypes.InterfaceElementPlatformType_PlatformType).getOneOrDefault(
            ArtifactReadable.SENTINEL);
      this.setApplicability(
         !art.getApplicabilityToken().getId().equals(-1L) ? art.getApplicabilityToken() : ApplicabilityToken.SENTINEL);
      if (pTypeArt.isValid() && !pTypeArt.getExistingAttributeTypes().isEmpty() && !(getInterfaceElementArrayHeader().isValid() && getInterfaceElementArrayHeader().getValue())) {
         PlatformTypeToken pType = new PlatformTypeToken(pTypeArt);
         this.setPlatformType(pType);
         if (pType.getEnumSet().isValid()) {
            this.setEnumLiteral(pType.getEnumSet().getDescription());
         }
         if (((getInterfaceDefaultValue().isValid() && getInterfaceDefaultValue().getValue().isEmpty()) || getInterfaceDefaultValue().isInvalid()) && (pType.getInterfaceDefaultValue() != null && pType.getInterfaceDefaultValue().isValid() && !pType.getInterfaceDefaultValue().getValue().isEmpty())) {
            this.setInterfaceDefaultValue(pType.getInterfaceDefaultValue().getValue());
         }
      } else if (getInterfaceElementArrayHeader().isValid() && getInterfaceElementArrayHeader().getValue()) {
         PlatformTypeToken arrayHeaderType = new PlatformTypeToken(0L, "Element Array Header", "", "0", "", "", "");
         arrayHeaderType.setDescription("");
         arrayHeaderType.setInterfaceDefaultValue("");
         arrayHeaderType.setInterfacePlatformType2sComplement(false);
         arrayHeaderType.setInterfacePlatformTypeAnalogAccuracy("");
         arrayHeaderType.setInterfacePlatformTypeBitsResolution("");
         arrayHeaderType.setInterfacePlatformTypeCompRate("");
         arrayHeaderType.setInterfacePlatformTypeMsbValue("");
         this.setPlatformType(arrayHeaderType);
         this.setInterfaceDefaultValue(arrayHeaderType.getInterfaceDefaultValue().getValue());
      } else {
         this.setPlatformType(PlatformTypeToken.SENTINEL);
         this.setInterfaceDefaultValue("");
         this.getPlatformType().setDescription("");
      }
      this.setArrayElements(
         art.getRelated(CoreRelationTypes.InterfaceElementArrayElement_ArrayElement).getList().stream().filter(
            a -> !a.getExistingAttributeTypes().isEmpty()).map(a -> new InterfaceStructureElementToken(a)).collect(
               Collectors.toList()));

      ArtifactReadable arrayDescptionSetArt =
         art.getRelated(CoreRelationTypes.InterfaceElementArrayIndexDescriptionSet_Set).getAtMostOneOrDefault(
            ArtifactReadable.SENTINEL);
      if (arrayDescptionSetArt.isValid() && !arrayDescptionSetArt.getExistingAttributeTypes().isEmpty()) {
         this.setArrayDescriptionSet(new InterfaceEnumerationSet(arrayDescptionSetArt));
      }
   }

   public InterfaceStructureElementToken(String name, String description, Double beginByte, Double beginWord, Integer size) {
      this(name, description, beginByte, beginWord, size, false);
   }

   public InterfaceStructureElementToken(String name, String description, Double beginByte, Double beginWord, Integer size, boolean offset) {
      super((long) -1, AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.Name, GammaId.SENTINEL, name, ""));
      this.setId((long) -1);
      this.setName(name);
      this.setDescription(description);
      this.setInterfaceElementAlterable(false);
      this.setInterfaceElementIndexStart(0);
      this.setInterfaceElementIndexEnd(size - 1);
      this.setNotes("");
      this.setBeginByte(beginByte);
      this.setBeginWord(beginWord);
      this.setApplicability(ApplicabilityToken.BASE);
      this.setInterfaceDefaultValue("");
      this.setAutogenerated(true);
      this.setIncludedInCounts(false); // Spares should not be included in structure byte counts
      this.setHasNegativeEndByteOffset(offset);
      this.getPlatformType().setDescription("Autogenerated upon page load");
      this.setEnumLiteral("");
      this.setInterfaceElementArrayHeader(false);
   }

   public InterfaceStructureElementToken(Long id, String name, ApplicabilityToken applicability, PlatformTypeToken pType) {
      super(id, AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.Name, GammaId.SENTINEL, name, ""));
      this.setDescription("");
      this.setNotes("");
      this.setInterfaceElementAlterable(false);
      this.setInterfaceElementIndexStart(0);
      this.setInterfaceElementIndexEnd(0);
      this.setApplicability(applicability);
      this.setPlatformType(pType);
      this.setAutogenerated(true);
      this.getPlatformType().setDescription("Autogenerated upon page load");
      this.setEnumLiteral("");
      this.setInterfaceElementArrayHeader(false);
   }

   /**
    * @param id
    * @param name
    */
   public InterfaceStructureElementToken(Long id, String name) {
      super(id, AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.Name, GammaId.SENTINEL, name, ""));
   }

   /**
    *
    */
   public InterfaceStructureElementToken() {
      super();
   }

   /**
    * @return the description
    */
   public AttributePojo<String> getDescription() {
      return Description;
   }

   /**
    * @param description the description to set
    */
   public void setDescription(String description) {
      this.Description =
         AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.Description, GammaId.SENTINEL, description, "");
   }

   @JsonProperty
   public void setDescription(AttributePojo<String> description) {
      this.Description = description;
   }

   /**
    * @return the notes
    */
   public AttributePojo<String> getNotes() {
      return Notes;
   }

   /**
    * @param notes the notes to set
    */
   public void setNotes(String notes) {
      Notes = AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.Notes, GammaId.SENTINEL, notes, "");
   }

   @JsonProperty
   public void setNotes(AttributePojo<String> notes) {
      this.Notes = notes;
   }

   /**
    * @return the interfaceElementAlterable
    */
   public AttributePojo<Boolean> getInterfaceElementAlterable() {
      return InterfaceElementAlterable;
   }

   /**
    * @param interfaceElementAlterable the interfaceElementAlterable to set
    */
   public void setInterfaceElementAlterable(Boolean interfaceElementAlterable) {
      InterfaceElementAlterable = AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementAlterable,
         GammaId.SENTINEL, interfaceElementAlterable, "");
   }

   @JsonProperty
   public void setInterfaceElementAlterable(AttributePojo<Boolean> interfaceElementAlterable) {
      InterfaceElementAlterable = interfaceElementAlterable;
   }

   public AttributePojo<Boolean> getInterfaceElementArrayHeader() {
      return interfaceElementArrayHeader;
   }

   public void setInterfaceElementArrayHeader(Boolean interfaceElementArrayHeader) {
      this.interfaceElementArrayHeader = AttributePojo.valueOf(Id.SENTINEL,
         CoreAttributeTypes.InterfaceElementArrayHeader, GammaId.SENTINEL, interfaceElementArrayHeader, "");
   }

   @JsonProperty
   public void setInterfaceElementArrayHeader(AttributePojo<Boolean> interfaceElementArrayHeader) {
      this.interfaceElementArrayHeader = interfaceElementArrayHeader;
   }

   public AttributePojo<Boolean> getInterfaceElementWriteArrayHeaderName() {
      return interfaceElementWriteArrayHeaderName;
   }

   public void setInterfaceElementWriteArrayHeaderName(Boolean interfaceElementWriteArrayHeaderName) {
      this.interfaceElementWriteArrayHeaderName =
         AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementWriteArrayHeaderName, GammaId.SENTINEL,
            interfaceElementWriteArrayHeaderName, "");
   }

   @JsonProperty
   public void setInterfaceElementWriteArrayHeaderName(AttributePojo<Boolean> interfaceElementWriteArrayHeaderName) {
      this.interfaceElementWriteArrayHeaderName = interfaceElementWriteArrayHeaderName;
   }

   public AttributePojo<String> getInterfaceElementArrayIndexDelimiterOne() {
      return interfaceElementArrayIndexDelimiterOne;
   }

   public void setInterfaceElementArrayIndexDelimiterOne(String interfaceElementArrayIndexDelimiterOne) {
      this.interfaceElementArrayIndexDelimiterOne =
         AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementArrayIndexDelimiterOne, GammaId.SENTINEL,
            interfaceElementArrayIndexDelimiterOne, "");
   }

   @JsonProperty
   public void setInterfaceElementArrayIndexDelimiterOne(AttributePojo<String> interfaceElementArrayIndexDelimiterOne) {
      this.interfaceElementArrayIndexDelimiterOne = interfaceElementArrayIndexDelimiterOne;
   }

   public AttributePojo<String> getInterfaceElementArrayIndexDelimiterTwo() {
      return interfaceElementArrayIndexDelimiterTwo;
   }

   public void setInterfaceElementArrayIndexDelimiterTwo(String interfaceElementArrayIndexDelimiterTwo) {
      this.interfaceElementArrayIndexDelimiterTwo =
         AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementArrayIndexDelimiterTwo, GammaId.SENTINEL,
            interfaceElementArrayIndexDelimiterTwo, "");
   }

   @JsonProperty
   public void setInterfaceElementArrayIndexDelimiterTwo(AttributePojo<String> interfaceElementArrayIndexDelimiterTwo) {
      this.interfaceElementArrayIndexDelimiterTwo = interfaceElementArrayIndexDelimiterTwo;
   }

   public AttributePojo<String> getInterfaceElementArrayIndexOrder() {
      return interfaceElementArrayIndexOrder;
   }

   public void setInterfaceElementArrayIndexOrder(String interfaceElementArrayIndexOrder) {
      this.interfaceElementArrayIndexOrder = AttributePojo.valueOf(Id.SENTINEL,
         CoreAttributeTypes.InterfaceElementArrayIndexOrder, GammaId.SENTINEL, interfaceElementArrayIndexOrder, "");
   }

   @JsonProperty
   public void setInterfaceElementArrayIndexOrder(AttributePojo<String> interfaceElementArrayIndexOrder) {
      this.interfaceElementArrayIndexOrder = interfaceElementArrayIndexOrder;
   }

   public void setInterfaceElementArrayIndexOrder(ElementArrayIndexOrder interfaceElementArrayIndexOrder) {
      this.interfaceElementArrayIndexOrder =
         AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementArrayIndexOrder, GammaId.SENTINEL,
            interfaceElementArrayIndexOrder.toString(), "");
   }

   /**
    * @return the interfaceElementIndexStart
    */
   public AttributePojo<Integer> getInterfaceElementIndexStart() {
      return InterfaceElementIndexStart;
   }

   /**
    * @param interfaceElementIndexStart the interfaceElementIndexStart to set
    */
   public void setInterfaceElementIndexStart(Integer interfaceElementIndexStart) {
      InterfaceElementIndexStart = AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementIndexStart,
         GammaId.SENTINEL, interfaceElementIndexStart, "");
   }

   @JsonProperty
   public void setInterfaceElementIndexStart(AttributePojo<Integer> interfaceElementIndexStart) {
      InterfaceElementIndexStart = interfaceElementIndexStart;
   }

   /**
    * @return the interfaceElementIndexEnd
    */
   public AttributePojo<Integer> getInterfaceElementIndexEnd() {
      return InterfaceElementIndexEnd;
   }

   /**
    * @param interfaceElementIndexEnd the interfaceElementIndexEnd to set
    */
   public void setInterfaceElementIndexEnd(Integer interfaceElementIndexEnd) {
      InterfaceElementIndexEnd = AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementIndexEnd,
         GammaId.SENTINEL, interfaceElementIndexEnd, "");
   }

   @JsonProperty
   public void setInterfaceElementIndexEnd(AttributePojo<Integer> interfaceElementIndexEnd) {
      InterfaceElementIndexEnd = interfaceElementIndexEnd;
   }

   /**
    * @return the beginByte
    */
   public Double getBeginByte() {
      return beginByte;
   }

   /**
    * @param beginByte the beginByte to set
    */
   public void setBeginByte(Double beginByte) {
      this.beginByte = beginByte;
   }

   /**
    * @return the endByte
    */
   public Double getEndByte() {
      return Math.max(0.0, (this.beginByte + this.getElementSizeInBytes() - 1) % (this.getValidationSize() / 2));
   }

   /**
    * @return the beginWord
    */
   public Double getBeginWord() {
      return beginWord;
   }

   /**
    * @param beginWord the beginWord to set
    */
   public void setBeginWord(Double beginWord) {
      this.beginWord = beginWord;
   }

   /**
    * @return the endWord
    */
   public Double getEndWord() {
      return Math.max(0.0, Math.ceil(
         ((this.getBeginWord() * (this.getValidationSize() / 2)) + this.getBeginByte() + this.getElementSizeInBytes()) / (this.getValidationSize() / 2)) - 1);
   }

   /**
    * @return the endByte, without resetting counter per word
    */
   @JsonIgnore
   public Double getEndingByteNoReset() {
      return this.getEndWord() * (this.getValidationSize() / 2);
   }

   /**
    * @return the endbit, without resetting counter per word
    */
   @JsonIgnore
   public Double getEndingBitNoReset() {
      return this.getEndingByteNoReset() * 8;
   }

   /**
    * @return the applicability
    */
   public ApplicabilityToken getApplicability() {
      return applicability;
   }

   /**
    * @param applicability the applicability to set
    */
   public void setApplicability(ApplicabilityToken applicability) {
      this.applicability = applicability;
   }

   /**
    * @return the InterfaceDefaultValue
    */
   public AttributePojo<String> getInterfaceDefaultValue() {
      return this.InterfaceDefaultValue;
   }

   /**
    * // * @param InterfaceDefaultValue the InterfaceDefaultValue to set
    */
   public void setInterfaceDefaultValue(String interfaceDefaultValue) {
      this.InterfaceDefaultValue = AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceDefaultValue,
         GammaId.SENTINEL, interfaceDefaultValue, "");
   }

   @JsonProperty
   public void setInterfaceDefaultValue(AttributePojo<String> interfaceDefaultValue) {
      this.InterfaceDefaultValue = interfaceDefaultValue;
   }

   /**
    * @return the autogenerated
    */
   public boolean isAutogenerated() {
      return autogenerated;
   }

   /**
    * @param autogenerated the autogenerated to set
    */
   public void setAutogenerated(boolean autogenerated) {
      this.autogenerated = autogenerated;
   }

   /**
    * @return the includedInCounts
    */
   @JsonIgnore
   public boolean isIncludedInCounts() {
      return includedInCounts;
   }

   /**
    * @param autogenerated the includedInCounts to set
    */
   @JsonIgnore
   public void setIncludedInCounts(boolean includedInCounts) {
      this.includedInCounts = includedInCounts;
   }

   /**
    * @return the interfacePlatformTypeBitSize
    */
   @JsonIgnore
   public double getInterfacePlatformTypeBitSize() {
      return Double.parseDouble(this.platformType.getInterfacePlatformTypeBitSize().getValue());
   }

   /**
    * @return the interfacePlatformTypeByteSize
    */
   @JsonIgnore
   public double getInterfacePlatformTypeByteSize() {
      return Double.parseDouble(this.platformType.getInterfacePlatformTypeBitSize().getValue()) / 8;

   }

   /**
    * @return the interfacePlatformTypeWordSize
    */
   @JsonIgnore
   public double getInterfacePlatformTypeWordSize() {
      return Math.floor(this.getInterfacePlatformTypeByteSize() / (this.getValidationSize() / 2));
   }

   /**
    * @return the length of array
    */
   @JsonIgnore
   public int getArrayLength() {
      return this.getInterfaceElementIndexEnd().getValue() - this.getInterfaceElementIndexStart().getValue() + 1;
   }

   /**
    * return size of element using array and type size
    */
   public double getElementSizeInBits() {
      if (this.getInterfaceElementArrayHeader().isValid() && this.getInterfaceElementArrayHeader().getValue()) {
         return getArrayLength() * getArrayElements().stream().filter(e -> e.isIncludedInCounts()).collect(
            Collectors.summingDouble(InterfaceStructureElementToken::getElementSizeInBits));
      }
      return (this.getArrayLength() * this.getInterfacePlatformTypeBitSize());
   }

   /**
    * return size of element using array and type size
    */
   public double getElementSizeInBytes() {
      if (this.getInterfaceElementArrayHeader().isValid() && this.getInterfaceElementArrayHeader().getValue() && getArrayElements().size() > 0) {
         return getArrayLength() * getArrayElements().stream().filter(e -> e.isIncludedInCounts()).collect(
            Collectors.summingDouble(InterfaceStructureElementToken::getElementSizeInBytes));
      }
      return this.getArrayLength() * this.getInterfacePlatformTypeBitSize() / 8;
   }

   /**
    * @return the hasNegativeEndByteOffset
    */
   @JsonIgnore
   public boolean isHasNegativeEndByteOffset() {
      return hasNegativeEndByteOffset;
   }

   /**
    * @param hasNegativeEndByteOffset the hasNegativeEndByteOffset to set
    */
   @JsonIgnore
   public void setHasNegativeEndByteOffset(boolean hasNegativeEndByteOffset) {
      this.hasNegativeEndByteOffset = hasNegativeEndByteOffset;
   }

   /**
    * @return the platformType
    */
   public PlatformTypeToken getPlatformType() {
      return platformType;
   }

   /**
    * @param platformType the platformType to set
    */
   public void setPlatformType(PlatformTypeToken platformType) {
      this.platformType = platformType;
   }

   public List<InterfaceStructureElementToken> getArrayElements() {
      return arrayElements;
   }

   public void setArrayElements(List<InterfaceStructureElementToken> arrayElements) {
      this.arrayElements = arrayElements;
   }

   /**
    * @return the enumLiteral
    */
   public AttributePojo<String> getEnumLiteral() {
      return enumLiteral;
   }

   /**
    * @param enumLiteral the enumLiteral to set
    */
   public void setEnumLiteral(String enumLiteral) {
      this.enumLiteral = AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementEnumLiteral,
         GammaId.SENTINEL, enumLiteral, "");
   }

   @JsonProperty
   public void setEnumLiteral(AttributePojo<String> enumLiteral) {
      this.enumLiteral = enumLiteral;
   }

   /**
    * @return the validationSize
    */
   @JsonIgnore
   public int getValidationSize() {
      return validationSize;
   }

   /**
    * @param validationSize the validationSize to set
    */
   @JsonIgnore
   public void setValidationSize(int validationSize) {
      this.validationSize = validationSize;
   }

   /**
    * @return the shouldValidate
    */
   @JsonIgnore
   public boolean isShouldValidate() {
      return shouldValidate;
   }

   /**
    * @param shouldValidate the shouldValidate to set
    */
   @JsonIgnore
   public void setShouldValidate(boolean shouldValidate) {
      this.shouldValidate = shouldValidate;
   }

   @JsonIgnore
   public boolean isArrayChild() {
      return isArrayChild;
   }

   public void setArrayChild(boolean isArrayChild) {
      this.isArrayChild = isArrayChild;
   }

   @JsonIgnore
   public InterfaceEnumerationSet getArrayDescriptionSet() {
      return arrayDescriptionSet;
   }

   public void setArrayDescriptionSet(InterfaceEnumerationSet arrayDescriptionSet) {
      this.arrayDescriptionSet = arrayDescriptionSet;
   }

   public AttributePojo<Boolean> isInterfaceElementBlockData() {
      return interfaceElementBlockData;
   }

   public AttributePojo<Boolean> getInterfaceElementBlockData() {
      return interfaceElementBlockData;
   }

   public void setInterfaceElementBlockData(Boolean interfaceElementBlockData) {
      this.interfaceElementBlockData = AttributePojo.valueOf(Id.SENTINEL, CoreAttributeTypes.InterfaceElementBlockData,
         GammaId.SENTINEL, interfaceElementBlockData, "");
   }

   @JsonProperty
   public void setInterfaceElementBlockData(AttributePojo<Boolean> interfaceElementBlockData) {
      this.interfaceElementBlockData = interfaceElementBlockData;
   }

   public CreateArtifact createArtifact(String key, ApplicabilityId applicId) {
      Map<AttributeTypeToken, String> values = new HashMap<>();
      values.put(CoreAttributeTypes.Description, this.getDescription().getValue());
      values.put(CoreAttributeTypes.InterfaceDefaultValue, this.getInterfaceDefaultValue().getValue());
      values.put(CoreAttributeTypes.InterfaceElementAlterable,
         this.getInterfaceElementAlterable().getValue().toString());
      values.put(CoreAttributeTypes.Notes, this.getNotes().getValue());
      values.put(CoreAttributeTypes.InterfaceElementEnumLiteral, this.getEnumLiteral().getValue());
      values.put(CoreAttributeTypes.InterfaceElementIndexStart,
         this.getInterfaceElementIndexStart().getValue().toString());
      values.put(CoreAttributeTypes.InterfaceElementIndexEnd, this.getInterfaceElementIndexEnd().getValue().toString());

      CreateArtifact art = new CreateArtifact();
      art.setName(this.getName().getValue());
      art.setTypeId(CoreArtifactTypes.InterfaceDataElement.getIdString());

      List<Attribute> attrs = new LinkedList<>();

      for (AttributeTypeToken type : CoreArtifactTypes.InterfaceDataElement.getValidAttributeTypes()) {
         String value = values.get(type);
         if (Strings.isInValid(value)) {
            continue;
         }
         Attribute attr = new Attribute(type.getIdString());
         attr.setValue(Arrays.asList(value));
         attrs.add(attr);
      }

      art.setAttributes(attrs);
      art.setApplicabilityId(applicId.getIdString());
      art.setkey(key);
      return art;
   }

   @JsonIgnore
   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj instanceof InterfaceStructureElementToken) {
         InterfaceStructureElementToken other = ((InterfaceStructureElementToken) obj);
         if (!this.getName().valueEquals(other.getName())) {
            return false;
         }
         if (!this.getDescription().valueEquals(other.getDescription())) {
            return false;
         }
         if (!this.getEnumLiteral().valueEquals(other.getEnumLiteral())) {
            return false;
         }
         if (!this.getInterfaceElementAlterable().valueEquals(other.getInterfaceElementAlterable())) {
            return false;
         }
         if (!this.getInterfaceElementArrayHeader().valueEquals(other.getInterfaceElementArrayHeader())) {
            return false;
         }
         if (!this.getInterfaceElementWriteArrayHeaderName().valueEquals(
            other.getInterfaceElementWriteArrayHeaderName())) {
            return false;
         }
         if (!this.getInterfaceElementArrayIndexOrder().valueEquals(other.getInterfaceElementArrayIndexOrder())) {
            return false;
         }
         if (!this.getInterfaceElementArrayIndexDelimiterOne().valueEquals(
            other.getInterfaceElementArrayIndexDelimiterOne())) {
            return false;
         }
         if (!this.getInterfaceElementArrayIndexDelimiterTwo().valueEquals(
            other.getInterfaceElementArrayIndexDelimiterTwo())) {
            return false;
         }
         if (!this.getNotes().valueEquals(other.getNotes())) {
            return false;
         }
         if (!this.getInterfaceElementIndexStart().valueEquals(other.getInterfaceElementIndexStart())) {
            return false;
         }
         if (!this.getInterfaceElementIndexEnd().valueEquals(other.getInterfaceElementIndexEnd())) {
            return false;
         }
         if (!this.getArrayElements().equals(other.getArrayElements())) {
            return false;
         }
         if (!this.getArrayDescriptionSet().equals(other.getArrayDescriptionSet())) {
            return false;
         }
         if (!this.getInterfaceElementBlockData().valueEquals(other.getInterfaceElementBlockData())) {
            return false;
         }
         if (!this.getInterfaceDefaultValue().valueEquals(other.getInterfaceDefaultValue())) {
            return false;
         }
         if (!this.getPlatformType().equals(other.getPlatformType())) {
            return false;
         }
         return true;
      }
      return false;

   }

}
