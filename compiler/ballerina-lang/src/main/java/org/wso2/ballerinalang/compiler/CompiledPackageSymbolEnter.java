/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.ballerinalang.compiler;

import org.ballerinalang.compiler.BLangCompilerException;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.MarkdownDocAttachment;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.NodeKind;
import org.wso2.ballerinalang.compiler.packaging.RepoHierarchy;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAttachedFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BConstantSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BErrorTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BStructureTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.TaintRecord;
import org.wso2.ballerinalang.compiler.semantics.model.types.BAnnotationType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BErrorType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFiniteType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BMapType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStreamType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStructureType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTupleType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.tree.BLangConstantValue;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangMapLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangConstRef;
import org.wso2.ballerinalang.compiler.util.BArrayState;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.DefaultValueLiteral;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.TypeDescriptor;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLog;
import org.wso2.ballerinalang.programfile.CompiledBinaryFile;
import org.wso2.ballerinalang.programfile.ConstantValue;
import org.wso2.ballerinalang.programfile.Instruction.RegIndex;
import org.wso2.ballerinalang.programfile.KeyInfo;
import org.wso2.ballerinalang.programfile.attributes.AttributeInfo;
import org.wso2.ballerinalang.programfile.attributes.AttributeInfo.Kind;
import org.wso2.ballerinalang.programfile.cpentries.BlobCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.ConstantPoolEntry;
import org.wso2.ballerinalang.programfile.cpentries.FloatCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.ForkJoinCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.FunctionRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.IntegerCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.MapCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.PackageRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.StringCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.StructureRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.TypeRefCPEntry;
import org.wso2.ballerinalang.programfile.cpentries.UTF8CPEntry;
import org.wso2.ballerinalang.programfile.cpentries.WorkerDataChannelRefCPEntry;
import org.wso2.ballerinalang.util.Flags;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

import static org.wso2.ballerinalang.compiler.semantics.model.Scope.NOT_FOUND_ENTRY;
import static org.wso2.ballerinalang.util.LambdaExceptionUtils.rethrow;

/**
 * This class is responsible for reading the compiled package file (balo) and creating a package symbol.
 * <p>
 * TODO This class contain duplicate code which we are planning to refactor
 *
 * @since 0.970.0
 */
public class CompiledPackageSymbolEnter {
    private final PackageLoader packageLoader;
    private final SymbolTable symTable;
    private final Names names;
    private final BLangDiagnosticLog dlog;
    private TypeSignatureReader<BType> typeSigReader;

    private CompiledPackageSymbolEnv env;

    private static final CompilerContext.Key<CompiledPackageSymbolEnter> COMPILED_PACKAGE_SYMBOL_ENTER_KEY =
            new CompilerContext.Key<>();

    public static CompiledPackageSymbolEnter getInstance(CompilerContext context) {
        CompiledPackageSymbolEnter packageReader = context.get(COMPILED_PACKAGE_SYMBOL_ENTER_KEY);
        if (packageReader == null) {
            packageReader = new CompiledPackageSymbolEnter(context);
        }

        return packageReader;
    }

    private CompiledPackageSymbolEnter(CompilerContext context) {
        context.put(COMPILED_PACKAGE_SYMBOL_ENTER_KEY, this);

        this.packageLoader = PackageLoader.getInstance(context);
        this.symTable = SymbolTable.getInstance(context);
        this.names = Names.getInstance(context);
        this.dlog = BLangDiagnosticLog.getInstance(context);
        this.typeSigReader = new TypeSignatureReader<>();
    }

    public BPackageSymbol definePackage(PackageID packageId,
                                        RepoHierarchy packageRepositoryHierarchy,
                                        byte[] packageBinaryContent) {
        BPackageSymbol pkgSymbol = definePackage(packageId, packageRepositoryHierarchy,
                new ByteArrayInputStream(packageBinaryContent));

        // Strip magic value (4 bytes) and the version (2 bytes) off from the binary content of the package.
        byte[] modifiedPkgBinaryContent = Arrays.copyOfRange(
                packageBinaryContent, 6, packageBinaryContent.length);
        pkgSymbol.packageFile = new CompiledBinaryFile.PackageFile(modifiedPkgBinaryContent);
        SymbolEnv builtinEnv = this.symTable.pkgEnvMap.get(symTable.langAnnotationModuleSymbol);
        SymbolEnv pkgEnv = SymbolEnv.createPkgEnv(null, pkgSymbol.scope, builtinEnv);
        this.symTable.pkgEnvMap.put(pkgSymbol, pkgEnv);
        return pkgSymbol;
    }

    public BPackageSymbol definePackage(PackageID packageId,
                                        RepoHierarchy packageRepositoryHierarchy,
                                        InputStream programFileInStream) {
        // TODO packageID --> package to be loaded. this is required for error reporting..
        try (DataInputStream dataInStream = new DataInputStream(programFileInStream)) {
            CompiledPackageSymbolEnv prevEnv = this.env;
            this.env = new CompiledPackageSymbolEnv();
            this.env.requestedPackageId = packageId;
            this.env.repoHierarchy = packageRepositoryHierarchy;
            BPackageSymbol pkgSymbol = definePackage(dataInStream);
            this.env = prevEnv;
            return pkgSymbol;
        } catch (IOException e) {
            // TODO dlog.error();
            throw new BLangCompilerException(e.getMessage(), e);
            //            return null;
        } catch (Throwable e) {
            // TODO format error
            throw new BLangCompilerException(e.getMessage(), e);
            //            return null;
        }
    }

    private BPackageSymbol definePackage(DataInputStream dataInStream) throws IOException {
        int magicNumber = dataInStream.readInt();
        if (magicNumber != CompiledBinaryFile.PackageFile.MAGIC_VALUE) {
            // TODO dlog.error() with package name
            throw new BLangCompilerException("invalid magic number " + magicNumber);
        }

        short version = dataInStream.readShort();
        if (version != CompiledBinaryFile.PackageFile.LANG_VERSION) {
            // TODO dlog.error() with package name
            throw new BLangCompilerException("unsupported program file version " + version);
        }

        // Read constant pool entries of the package info.
        this.env.constantPool = readConstantPool(dataInStream);

        // Read packageName and version
        String orgName = getUTF8CPEntryValue(dataInStream);
        String pkgName = getUTF8CPEntryValue(dataInStream);
        String pkgVersion = getUTF8CPEntryValue(dataInStream);
        return definePackage(dataInStream, orgName, pkgName, pkgVersion);
    }

    private BPackageSymbol definePackage(DataInputStream dataInStream,
                                         String orgName,
                                         String pkgName,
                                         String pkgVersion) throws IOException {

        PackageID pkgId = createPackageID(orgName, pkgName, pkgVersion);
        this.env.pkgSymbol = Symbols.createPackageSymbol(pkgId, this.symTable);

        // TODO Validate this pkdID with the requestedPackageID available in the env.

        // Define import packages.
        defineSymbols(dataInStream, rethrow(this::defineImportPackage));

        // Define type definitions.
        defineSymbols(dataInStream, rethrow(this::defineTypeDef));

        // Define annotations.
        defineSymbols(dataInStream, rethrow(this::defineAnnotations));

        // Define services.
        defineSymbols(dataInStream, rethrow(this::defineService));

        // Resolve unresolved types.
        resolveTypes();

        // Read resource info entries.
        defineSymbols(dataInStream, rethrow(this::defineResource));

        // Define constants.
        defineSymbols(dataInStream, rethrow(this::defineConstants));

        // Set symbol of constant references if needed.
        updateUnresolvedConstantReferences();

        // Define package level variables.
        defineSymbols(dataInStream, rethrow(this::definePackageLevelVariables));

        // Define functions.
        defineSymbols(dataInStream, rethrow(this::defineFunction));
        assignInitFunctions();

        // Read package level attributes
        readAttributes(dataInStream);

        return this.env.pkgSymbol;
    }

    private ConstantPoolEntry[] readConstantPool(DataInputStream dataInStream) throws IOException {
        int constantPoolSize = dataInStream.readInt();
        ConstantPoolEntry[] constantPool = new ConstantPoolEntry[constantPoolSize];
        for (int i = 0; i < constantPoolSize; i++) {
            byte cpTag = dataInStream.readByte();
            ConstantPoolEntry.EntryType cpEntryType = ConstantPoolEntry.EntryType.values()[cpTag - 1];
            constantPool[i] = readCPEntry(dataInStream, constantPool, cpEntryType);
        }
        return constantPool;
    }

    private ConstantPoolEntry readCPEntry(DataInputStream dataInStream,
                                          ConstantPoolEntry[] constantPool,
                                          ConstantPoolEntry.EntryType cpEntryType) throws IOException {
        int cpIndex;
        int pkgCPIndex;
        UTF8CPEntry utf8CPEntry;
        switch (cpEntryType) {
            case CP_ENTRY_UTF8:
                short length = dataInStream.readShort();
                String strValue = null;

                // If the length of the bytes is -1, that means no UTF value has been written.
                // i.e: string value represented by the UTF should be null.
                // Therefore we read the UTF value only if the length >= 0.
                if (length >= 0) {
                    strValue = dataInStream.readUTF();
                }
                return new UTF8CPEntry(strValue);
            case CP_ENTRY_INTEGER:
                return new IntegerCPEntry(dataInStream.readLong());
            case CP_ENTRY_FLOAT:
                return new FloatCPEntry(dataInStream.readDouble());
            case CP_ENTRY_STRING:
                cpIndex = dataInStream.readInt();
                utf8CPEntry = (UTF8CPEntry) constantPool[cpIndex];
                return new StringCPEntry(cpIndex, utf8CPEntry.getValue());
            case CP_ENTRY_BLOB:
                int blobLength = dataInStream.readInt();
                byte[] blobValue = new byte[blobLength];
                dataInStream.readFully(blobValue);
                return new BlobCPEntry(blobValue);
            case CP_ENTRY_PACKAGE:
                cpIndex = dataInStream.readInt();
                int versionCPIndex = dataInStream.readInt();
                return new PackageRefCPEntry(cpIndex, versionCPIndex);
            case CP_ENTRY_FUNCTION_REF:
                pkgCPIndex = dataInStream.readInt();
                cpIndex = dataInStream.readInt();
                return new FunctionRefCPEntry(pkgCPIndex, cpIndex);
            case CP_ENTRY_STRUCTURE_REF:
                pkgCPIndex = dataInStream.readInt();
                cpIndex = dataInStream.readInt();
                return new StructureRefCPEntry(pkgCPIndex, cpIndex);
            case CP_ENTRY_TYPE_REF:
                int typeSigCPIndex = dataInStream.readInt();
                return new TypeRefCPEntry(typeSigCPIndex);
            case CP_ENTRY_FORK_JOIN:
                int forkJoinCPIndex = dataInStream.readInt();
                return new ForkJoinCPEntry(forkJoinCPIndex);
            case CP_ENTRY_WRKR_DATA_CHNL_REF:
                int uniqueNameCPIndex = dataInStream.readInt();
                UTF8CPEntry wrkrDtChnlTypesSigCPEntry = (UTF8CPEntry) constantPool[uniqueNameCPIndex];
                return new WorkerDataChannelRefCPEntry(uniqueNameCPIndex, wrkrDtChnlTypesSigCPEntry.getValue());
            case CP_ENTRY_MAP:
                return readMapConstantPoolEntry(dataInStream, constantPool);
            default:
                throw new BLangCompilerException("invalid constant pool entry " + cpEntryType.getValue());
        }
    }

    private ConstantPoolEntry readMapConstantPoolEntry(DataInputStream dataInStream,
                                                       ConstantPoolEntry[] constantPool) throws IOException {
        LinkedHashMap<KeyInfo, ConstantValue> valueMap = new LinkedHashMap<>();

        // Read the size of the constant value map.
        int size = dataInStream.readInt();

        for (int i = 0; i < size; i++) {

            // Read the CP index of the key.
            int keyCPIndex = dataInStream.readInt();
            UTF8CPEntry keyCPEntry = (UTF8CPEntry) constantPool[keyCPIndex];

            boolean isSimpleLiteral = dataInStream.readBoolean();
            if (isSimpleLiteral) {
                // Read value type tag.
                int typeTag = dataInStream.readInt();

                KeyInfo keyInfo = new KeyInfo(keyCPEntry.getValue());

                ConstantValue constantValue = new ConstantValue();
                constantValue.literalValueTypeTag = typeTag;
                constantValue.isSimpleLiteral = true;

                // Read the value accordingly.
                if (typeTag == TypeTags.NIL) {
                    // Do nothing.
                } else if (typeTag == TypeTags.BOOLEAN) {
                    constantValue.booleanValue = dataInStream.readBoolean();
                    valueMap.put(keyInfo, constantValue);
                } else {
                    constantValue.valueCPEntryIndex = dataInStream.readInt();
                    valueMap.put(keyInfo, constantValue);
                }
            } else {
                int valueCPIndex = dataInStream.readInt();

                MapCPEntry mapCPEntry = (MapCPEntry) constantPool[valueCPIndex];

                KeyInfo keyInfo = new KeyInfo(keyCPEntry.getValue());

                ConstantValue constantValue = new ConstantValue();
                constantValue.valueCPEntryIndex = valueCPIndex;
                constantValue.constantValueMap = mapCPEntry.getConstantValueMap();

                valueMap.put(keyInfo, constantValue);
            }
        }

        return new MapCPEntry(null, valueMap);
    }

    private void defineSymbols(DataInputStream dataInStream,
                               Consumer<DataInputStream> symbolDefineFunc) throws IOException {
        int symbolCount = dataInStream.readShort();
        for (int i = 0; i < symbolCount; i++) {
            symbolDefineFunc.accept(dataInStream);
        }
    }

    // TODO do we need to load all the import packages of a compiled package.
    private void defineImportPackage(DataInputStream dataInStream) throws IOException {
        String orgName = getUTF8CPEntryValue(dataInStream);
        String pkgName = getUTF8CPEntryValue(dataInStream);
        String pkgVersion = getUTF8CPEntryValue(dataInStream);
        PackageID importPkgID = createPackageID(orgName, pkgName, pkgVersion);
        BPackageSymbol importPackageSymbol = packageLoader.loadPackageSymbol(importPkgID, this.env.pkgSymbol.pkgID,
                this.env.repoHierarchy);

        if (importPackageSymbol == null) {
            throw new BLangCompilerException("missing symbol in " + this.env.pkgSymbol + " for import " + importPkgID);
        }
        //TODO: after balo_change try to not to add to scope, it's duplicated with 'imports'
        // Define the import package with the alias being the package name
        this.env.pkgSymbol.scope.define(importPkgID.name, importPackageSymbol);
        this.env.pkgSymbol.imports.add(importPackageSymbol);
    }

    private void defineFunction(DataInputStream dataInStream) throws IOException {
        // Consider attached functions.. remove the first variable
        String funcName = getUTF8CPEntryValue(dataInStream);
        String funcSig = getUTF8CPEntryValue(dataInStream);
        int flags = dataInStream.readInt();

        BInvokableType funcType = createInvokableType(funcSig);
        BInvokableSymbol invokableSymbol = Symbols.createFunctionSymbol(flags, names.fromString(funcName),
                this.env.pkgSymbol.pkgID, funcType, this.env.pkgSymbol, Symbols.isFlagOn(flags, Flags.NATIVE));
        invokableSymbol.retType = funcType.retType;
        Scope scopeToDefine = this.env.pkgSymbol.scope;

        if (Symbols.isFlagOn(flags, Flags.ATTACHED)) {
            int attachedToTypeRefCPIndex = dataInStream.readInt();
            TypeRefCPEntry typeRefCPEntry = (TypeRefCPEntry) this.env.constantPool[attachedToTypeRefCPIndex];
            UTF8CPEntry typeSigCPEntry = (UTF8CPEntry) this.env.constantPool[typeRefCPEntry.typeSigCPIndex];
            BType attachedType = getBTypeFromDescriptor(typeSigCPEntry.getValue());

            // Update the symbol
            invokableSymbol.owner = attachedType.tsymbol;
            invokableSymbol.name =
                    names.fromString(Symbols.getAttachedFuncSymbolName(attachedType.tsymbol.name.value, funcName));
            if (attachedType.tag == TypeTags.OBJECT || attachedType.tag == TypeTags.RECORD) {
                if (attachedType.tag == TypeTags.OBJECT) {
                    scopeToDefine = ((BObjectTypeSymbol) attachedType.tsymbol).methodScope;
                } else {
                    scopeToDefine = attachedType.tsymbol.scope;
                }
                BAttachedFunction attachedFunc =
                        new BAttachedFunction(names.fromString(funcName), invokableSymbol, funcType);
                BStructureTypeSymbol structureTypeSymbol = (BStructureTypeSymbol) attachedType.tsymbol;
                structureTypeSymbol.attachedFuncs.add(attachedFunc);
                if (Names.USER_DEFINED_INIT_SUFFIX.value.equals(funcName)
                        || funcName.equals(Names.INIT_FUNCTION_SUFFIX.value)) {
                    structureTypeSymbol.initializerFunc = attachedFunc;
                }
            }
        }

        // Read and ignore worker data
        int noOfWorkerDataBytes = dataInStream.readInt();
        if (noOfWorkerDataBytes > 0) {
            byte[] workerData = new byte[noOfWorkerDataBytes];
            int bytesRead = dataInStream.read(workerData);
            if (bytesRead != noOfWorkerDataBytes) {
                // TODO throw an error
            }
        }

        // Read attributes
        Map<Kind, byte[]> attrDataMap = readAttributes(dataInStream);

        // set parameter symbols to the function symbol
        setParamSymbols(invokableSymbol, attrDataMap);

        // set taint table to the function symbol
        setTaintTable(invokableSymbol, attrDataMap);

        setDocumentation(invokableSymbol, attrDataMap);

        scopeToDefine.define(invokableSymbol.name, invokableSymbol);
    }

    private void defineTypeDef(DataInputStream dataInStream) throws IOException {
        String typeDefName = getUTF8CPEntryValue(dataInStream);
        int flags = dataInStream.readInt();
        boolean isLabel = dataInStream.readBoolean();
        int typeTag = dataInStream.readInt();

        BTypeSymbol typeDefSymbol;

        if (isLabel) {
            typeDefSymbol = readLabelTypeSymbol(dataInStream, typeDefName, flags);
            // Read and ignore attributes
            readAttributes(dataInStream);

            this.env.pkgSymbol.scope.define(typeDefSymbol.name, typeDefSymbol);
            return;
        }

        switch (typeTag) {
            case TypeTags.OBJECT:
                typeDefSymbol = readObjectTypeSymbol(dataInStream, typeDefName, flags);
                break;
            case TypeTags.RECORD:
                typeDefSymbol = readRecordTypeSymbol(dataInStream, typeDefName, flags);
                break;
            case TypeTags.ERROR:
                typeDefSymbol = readErrorTypeSymbol(dataInStream, typeDefName, flags);
                break;
            case TypeTags.FINITE:
                typeDefSymbol = readFiniteTypeSymbol(dataInStream, typeDefName, flags);
                break;
            default:
                typeDefSymbol = readLabelTypeSymbol(dataInStream, typeDefName, flags);
        }

        // Read and ignore attributes
        Map<Kind, byte[]> attrDataMap = readAttributes(dataInStream);

        setDocumentation(typeDefSymbol, attrDataMap);

        this.env.pkgSymbol.scope.define(typeDefSymbol.name, typeDefSymbol);
    }

    private void defineAnnotations(DataInputStream dataInStream) throws IOException {
        String name = getUTF8CPEntryValue(dataInStream);
        int flags = dataInStream.readInt();
        int attachPoints = dataInStream.readInt();
        int typeSig = dataInStream.readInt();

        BSymbol annotationSymbol = Symbols.createAnnotationSymbol(flags, attachPoints, names.fromString(name),
                this.env.pkgSymbol.pkgID, null, this.env.pkgSymbol);
        annotationSymbol.type = new BAnnotationType((BAnnotationSymbol) annotationSymbol);

        this.env.pkgSymbol.scope.define(annotationSymbol.name, annotationSymbol);
        if (typeSig > 0) {
            UTF8CPEntry typeSigCPEntry = (UTF8CPEntry) this.env.constantPool[typeSig];
            BType varType = getBTypeFromDescriptor(typeSigCPEntry.getValue());
            ((BAnnotationSymbol) annotationSymbol).attachedType = varType.tsymbol;
        }
    }

    private BObjectTypeSymbol readObjectTypeSymbol(DataInputStream dataInStream,
                                                   String name, int flags) throws IOException {
        BObjectTypeSymbol symbol = (BObjectTypeSymbol) Symbols.createObjectSymbol(flags, names.fromString(name),
                this.env.pkgSymbol.pkgID, null, this.env.pkgSymbol);
        symbol.scope = new Scope(symbol);
        symbol.methodScope = new Scope(symbol);
        BObjectType type = new BObjectType(symbol);
        symbol.type = type;

        // Define Object Fields
        defineSymbols(dataInStream, rethrow(dataInputStream ->
                defineStructureField(dataInStream, symbol, type)));

        // Read and ignore attributes
        readAttributes(dataInStream);

        return symbol;
    }

    private BRecordTypeSymbol readRecordTypeSymbol(DataInputStream dataInStream,
                                                   String name, int flags) throws IOException {
        BRecordTypeSymbol symbol = Symbols.createRecordSymbol(flags, names.fromString(name),
                this.env.pkgSymbol.pkgID, null,
                this.env.pkgSymbol);
        symbol.scope = new Scope(symbol);
        BRecordType type = new BRecordType(symbol);
        symbol.type = type;

        type.sealed = dataInStream.readBoolean();
        if (!type.sealed) {
            String restFieldTypeDesc = getUTF8CPEntryValue(dataInStream);
            UnresolvedType restFieldType = new UnresolvedType(restFieldTypeDesc,
                                                              restType -> type.restFieldType = restType);
            this.env.unresolvedTypes.add(restFieldType);
        } else {
            type.restFieldType = symTable.noType;
        }

        // Define Object Fields
        defineSymbols(dataInStream, rethrow(dataInputStream ->
                defineStructureField(dataInStream, symbol, type)));

        // Read and ignore attributes
        readAttributes(dataInStream);

        return symbol;
    }


    private BErrorTypeSymbol readErrorTypeSymbol(DataInputStream dataInStream, String name, int flags)
            throws  IOException {
        BErrorTypeSymbol symbol = Symbols.createErrorSymbol(flags, names.fromString(name), this.env.pkgSymbol.pkgID,
                                                            null, this.env.pkgSymbol);
        symbol.scope = new Scope(symbol);
        BErrorType type = new BErrorType(symbol);
        symbol.type = type;

        String reasonTypeDesc = getUTF8CPEntryValue(dataInStream);
        UnresolvedType reasonType = new UnresolvedType(reasonTypeDesc,
                                                       errReasonType -> type.reasonType = errReasonType);
        this.env.unresolvedTypes.add(reasonType);

        String detailTypeDesc = getUTF8CPEntryValue(dataInStream);
        UnresolvedType detailType = new UnresolvedType(detailTypeDesc,
                                                       errDetailType -> type.detailType = errDetailType);
        this.env.unresolvedTypes.add(detailType);

        // Read and ignore attributes
        readAttributes(dataInStream);

        return symbol;
    }

    private BTypeSymbol readFiniteTypeSymbol(DataInputStream dataInStream,
                                             String name, int flags) throws IOException {
        BTypeSymbol symbol = Symbols.createTypeSymbol(SymTag.FINITE_TYPE, flags, names.fromString(name),
                this.env.pkgSymbol.pkgID, null, this.env.pkgSymbol);
        symbol.scope = new Scope(symbol);
        BFiniteType finiteType = new BFiniteType(symbol);
        symbol.type = finiteType;

        // Define Object Fields
        defineSymbols(dataInStream, rethrow(dataInputStream ->
                defineValueSpace(dataInStream, finiteType)));

        return symbol;
    }

    private BTypeSymbol readLabelTypeSymbol(DataInputStream dataInStream,
                                            String name, int flags) throws IOException {
        String typeSig = getUTF8CPEntryValue(dataInStream);
        BType type = getBTypeFromDescriptor(typeSig);

        BTypeSymbol symbol = type.tsymbol.createLabelSymbol();
        symbol.type = type;

        symbol.name = names.fromString(name);
        symbol.pkgID = this.env.pkgSymbol.pkgID;
        symbol.flags = flags;

        return symbol;
    }

    private void defineValueSpace(DataInputStream dataInStream, BFiniteType finiteType) throws IOException {
        int typeDescCPIndex = dataInStream.readInt();
        UTF8CPEntry typeDescCPEntry = (UTF8CPEntry) this.env.constantPool[typeDescCPIndex];
        String typeDesc = typeDescCPEntry.getValue();

        BLangLiteral litExpr = createLiteralBasedOnDescriptor(typeDesc);

        int valueCPIndex;
        switch (typeDesc) {
            case TypeDescriptor.SIG_BOOLEAN:
                litExpr.value = dataInStream.readBoolean();
                break;
            case TypeDescriptor.SIG_INT:
            case TypeDescriptor.SIG_BYTE:
                valueCPIndex = dataInStream.readInt();
                IntegerCPEntry integerCPEntry = (IntegerCPEntry) this.env.constantPool[valueCPIndex];
                litExpr.value = integerCPEntry.getValue();
                break;
            case TypeDescriptor.SIG_FLOAT:
                valueCPIndex = dataInStream.readInt();
                FloatCPEntry floatCPEntry = (FloatCPEntry) this.env.constantPool[valueCPIndex];
                litExpr.value = Double.toString(floatCPEntry.getValue());
                break;
            case TypeDescriptor.SIG_DECIMAL:
                valueCPIndex = dataInStream.readInt();
                UTF8CPEntry decimalEntry = (UTF8CPEntry) this.env.constantPool[valueCPIndex];
                litExpr.value = decimalEntry.getValue();
                break;
            case TypeDescriptor.SIG_STRING:
                valueCPIndex = dataInStream.readInt();
                UTF8CPEntry stringCPEntry = (UTF8CPEntry) this.env.constantPool[valueCPIndex];
                litExpr.value = stringCPEntry.getValue();
                break;
            case TypeDescriptor.SIG_NULL:
                break;
            default:
                throw new BLangCompilerException("unknown default value type " + typeDesc);
        }

        litExpr.type = getBTypeFromDescriptor(typeDesc);

        finiteType.valueSpace.add(litExpr);
    }

    private void defineStructureField(DataInputStream dataInStream,
                                      BTypeSymbol objectSymbol,
                                      BStructureType objectType) throws IOException {
        String fieldName = getUTF8CPEntryValue(dataInStream);
        String typeSig = getUTF8CPEntryValue(dataInStream);
        int flags = dataInStream.readInt();
        int memIndex = dataInStream.readInt();

        BVarSymbol varSymbol = new BVarSymbol(flags, names.fromString(fieldName),
                objectSymbol.pkgID, null, objectSymbol.scope.owner);
        objectSymbol.scope.define(varSymbol.name, varSymbol);

        // Read the default value attribute
        Map<AttributeInfo.Kind, byte[]> attrData = readAttributes(dataInStream);

        // The object field type cannot be resolved now. Hence add it to the unresolved type list.
        UnresolvedType unresolvedFieldType = new UnresolvedType(typeSig, type -> {
            varSymbol.type = type;
            varSymbol.varIndex = new RegIndex(memIndex, type.tag);
            // Here the field doesn't need the position as it comes from a compiled package.
            BField structField = new BField(varSymbol.name, null, varSymbol);
            objectType.fields.add(structField);
        });

        setDocumentation(varSymbol, attrData);

        this.env.unresolvedTypes.add(unresolvedFieldType);
    }

    private void defineService(DataInputStream dataInStream) throws IOException {
        dataInStream.readInt();
        dataInStream.readInt();
        dataInStream.readInt();
    }

    private void defineResource(DataInputStream dataInStream) throws IOException {
        int resourceCount = dataInStream.readShort();
        for (int j = 0; j < resourceCount; j++) {
            dataInStream.readInt();
        }
    }

    private void defineConstants(DataInputStream dataInStream) throws IOException {
        // Get the name.
        String constantName = getUTF8CPEntryValue(dataInStream);
        // Get the flags.
        int flags = dataInStream.readInt();

        Scope enclScope = this.env.pkgSymbol.scope;
        BConstantSymbol constantSymbol;

        boolean isSimpleLiteral = dataInStream.readBoolean();
        if (isSimpleLiteral) {
            // Get symbol's type.
            String symbolTypeSig = getUTF8CPEntryValue(dataInStream);
            BType symbolType = getBTypeFromDescriptor(symbolTypeSig);

            // Get value literal's type.
            String valueTypeSig = getUTF8CPEntryValue(dataInStream);
            BType valueType = getBTypeFromDescriptor(valueTypeSig);

            // Get the simple literal value.
            Object object = readSimpleLiteralValue(dataInStream, valueType.tag);

            // Create the constant symbol.
            constantSymbol = new BConstantSymbol(flags, names.fromString(constantName), this.env.pkgSymbol.pkgID,
                    valueType, symbolType, enclScope.owner);
            constantSymbol.value = new BLangConstantValue(object, valueType);
        } else {
            // Read value type. Don't need the finite type since the literal is not a simple literal.
            String valueTypeSig = getUTF8CPEntryValue(dataInStream);
            BType valueType = getBTypeFromDescriptor(valueTypeSig);

            // Create the constant symbol.
            constantSymbol = new BConstantSymbol(flags, names.fromString(constantName), this.env.pkgSymbol.pkgID,
                    valueType, valueType, enclScope.owner);

            // Read the constant value CP entry index.
            int constantValueCPEntry = dataInStream.readInt();

            // Get the corresponding MapCPEntry.
            MapCPEntry mapCPEntry = (MapCPEntry) this.env.constantPool[constantValueCPEntry];

            // Since this is a top level map literal, set the symbol to the MapCPEntry. This will be later used to
            // identify references.
            mapCPEntry.setConstantSymbol(constantSymbol);

            // Read the map literal.
            BLangMapLiteral mapLiteral = readConstantValueMap(dataInStream, valueType);

            // If the mapCPEntry does not contain a literalValue, that means we are encountering this value for the
            // first time. Then we update the constant symbol's literal value and the mapCPEntries literal value with
            // the map literal which we have read.
            if (mapCPEntry.literalValue == null) {
                // constantSymbol.value = mapCPEntry.literalValue = mapLiteral;
            } else {
                // If the mapCPEntry's literal value is not null, that means we have encountered this value
                // earlier. In such case, set the mapCPEntry's literal value as the constant symbol's literal value.
                // This is done to make sure all the references have the same literal value. Otherwise the `===` will
                // fail for them.
                // constantSymbol.value = mapCPEntry.literalValue;
            }

            constantSymbol.cpEntryIndex = constantValueCPEntry;
        }

        // Define constant.
        enclScope.define(constantSymbol.name, constantSymbol);
        // Read attributes.
        Map<Kind, byte[]> attrDataMap = readAttributes(dataInStream);
        // Set documentations.
        setDocumentation(constantSymbol, attrDataMap);
    }

    private Object readSimpleLiteralValue(DataInputStream dataInStream, int typeTag) throws IOException {
        // Get the value.
        int valueCPIndex;
        switch (typeTag) {
            case TypeTags.BOOLEAN:
                return dataInStream.readBoolean();
            case TypeTags.BYTE:
            case TypeTags.INT:
                valueCPIndex = dataInStream.readInt();
                IntegerCPEntry integerCPEntry = (IntegerCPEntry) this.env.constantPool[valueCPIndex];
                return integerCPEntry.getValue();
            case TypeTags.FLOAT:
                valueCPIndex = dataInStream.readInt();
                FloatCPEntry floatCPEntry = (FloatCPEntry) this.env.constantPool[valueCPIndex];
                return floatCPEntry.getValue();
            case TypeTags.DECIMAL:
                valueCPIndex = dataInStream.readInt();
                UTF8CPEntry decimalEntry = (UTF8CPEntry) this.env.constantPool[valueCPIndex];
                return decimalEntry.getValue();
            case TypeTags.STRING:
                valueCPIndex = dataInStream.readInt();
                UTF8CPEntry stringCPEntry = (UTF8CPEntry) this.env.constantPool[valueCPIndex];
                return stringCPEntry.getValue();
            case TypeTags.NIL:
                return null;
            default:
                throw new RuntimeException("unexpected type tag: " + typeTag);
        }
    }

    private BLangLiteral readSimpleLiteral(DataInputStream dataInStream) throws IOException {
        String valueTypeSig = getUTF8CPEntryValue(dataInStream);
        BType valueType = getBTypeFromDescriptor(valueTypeSig);

        int typeTag = valueType.tag;

        // Read the value.
        Object value = readSimpleLiteralValue(dataInStream, typeTag);

        // Create a new literal.
        BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
        literal.value = value;
        literal.type = symTable.getTypeFromTag(typeTag);

        return literal;
    }

    private BLangMapLiteral readConstantValueMap(DataInputStream dataInStream, BType type) throws IOException {

        LinkedList<BLangRecordLiteral.BLangRecordKeyValue> keyValues = new LinkedList<>();

        // Read the map literal size.
        int size = dataInStream.readInt();
        for (int i = 0; i < size; i++) {
            String key = getUTF8CPEntryValue(dataInStream);

            boolean isSimpleLiteral = dataInStream.readBoolean();
            boolean isConstRef = dataInStream.readBoolean();

            // Get the value.
            BLangExpression value;
            if (isSimpleLiteral) {
                // Read the simple literal.
                value = readSimpleLiteral(dataInStream);
            } else {
                // Get the type of the record literal.
                String valueTypeSig = getUTF8CPEntryValue(dataInStream);
                BType valueType = getBTypeFromDescriptor(valueTypeSig);

                int valueCPEntryIndex = dataInStream.readInt();
                MapCPEntry mapCPEntry = (MapCPEntry) this.env.constantPool[valueCPEntryIndex];

                BLangMapLiteral recordLiteral = readConstantValueMap(dataInStream, valueType);

                // If the current map entry is a reference value, create a new BLangConstRef and set the symbol which
                // is retrieved from the mapCPEntry (in defineConstants we set the symbol). But sometimes this symbol
                // can be null because we might encounter the reference before the constant definition.
                //
                // Eg - const map<map<boolean>> bm3 = { "key2": bm1 };
                //      const map<boolean> bm1 = { "key1": true };
                //
                // In such situations, we need to update the BLangConstRef's symbol with the constant symbol once we
                // create a symbol for that. In such cases, we add the BLangConstRef and the mapCPEntry to a map and
                // after reading all of the constants, we iterate through them and update the symbol of
                // BLangConstRef's accordingly.
                if (isConstRef) {
                    BLangConstRef constRef = new BLangConstRef(mapCPEntry.getConstantSymbol());
                    constRef.desugared = true;
                    constRef.type = valueType;

                    value = constRef;

                    // Add the BLangConstRef and the corresponding MapCPEntry to the map so we can properly update
                    // them later.
                    this.env.unresolvedConstReferences.put(constRef, mapCPEntry);
                } else {
                    // If it is not a constant reference, we update the mapCPEntry's value.
                    value = mapCPEntry.literalValue = recordLiteral;
                }
            }
            // Create a new literal for the key.
            BLangLiteral keyLiteral = (BLangLiteral) TreeBuilder.createLiteralExpression();
            keyLiteral.value = key;
            keyLiteral.type = symTable.stringType;

            // Create a new key-value.
            BLangRecordLiteral.BLangRecordKeyValue recordKeyValue = new BLangRecordLiteral.BLangRecordKeyValue();
            recordKeyValue.key = new BLangRecordLiteral.BLangRecordKey(keyLiteral);
            recordKeyValue.valueExpr = value;

            keyValues.push(recordKeyValue);
        }
        // Create a new map literal.
        return new BLangRecordLiteral.BLangMapLiteral(null, keyValues, type);
    }

    private void definePackageLevelVariables(DataInputStream dataInStream) throws IOException {
        String varName = getUTF8CPEntryValue(dataInStream);
        String typeSig = getUTF8CPEntryValue(dataInStream);
        int flags = dataInStream.readInt();
        int memIndex = dataInStream.readInt();

        // Read and ignore identifier kind flag
        dataInStream.readBoolean();

        Map<Kind, byte[]> attrDataMap = readAttributes(dataInStream);

        // Create variable symbol
        BType varType = getBTypeFromDescriptor(typeSig);
        Scope enclScope = this.env.pkgSymbol.scope;
        BVarSymbol varSymbol;

        if (varType.tag == TypeTags.INVOKABLE) {
            // Here we don't set the required-params, defaultable params and the rest param of
            // the symbol. Because, for the function pointers we directly read the param types
            // from the varType (i.e: from InvokableType), and assumes it can have only required
            // params.
            varSymbol = new BInvokableSymbol(SymTag.VARIABLE, flags, names.fromString(varName),
                    this.env.pkgSymbol.pkgID, varType, enclScope.owner);
        } else {
            varSymbol = new BVarSymbol(flags, names.fromString(varName), this.env.pkgSymbol.pkgID, varType,
                    enclScope.owner);
            if (Symbols.isFlagOn(varType.tsymbol.flags, Flags.CLIENT)) {
                varSymbol.tag = SymTag.ENDPOINT;
            }
        }

        setDocumentation(varSymbol, attrDataMap);

        varSymbol.varIndex = new RegIndex(memIndex, varType.tag);
        enclScope.define(varSymbol.name, varSymbol);
    }

    private Map<AttributeInfo.Kind, byte[]> readAttributes(DataInputStream dataInStream) throws IOException {
        int attributesCount = dataInStream.readShort();
        if (attributesCount == 0) {
            return new HashMap<>();
        }

        Map<AttributeInfo.Kind, byte[]> attrDataMap = new HashMap<>(attributesCount);
        for (int i = 0; i < attributesCount; i++) {
            String attrName = getUTF8CPEntryValue(dataInStream);
            AttributeInfo.Kind attrKind = AttributeInfo.Kind.fromString(attrName);
            if (attrKind == null) {
                // TODO use dlog....
                throw new BLangCompilerException("unknown attribute kind " + attrName);
            }

            int noOfAttrDataBytes = dataInStream.readInt();
            byte[] attrData = new byte[noOfAttrDataBytes];
            int noOfBytesRead = dataInStream.read(attrData);
            if (noOfAttrDataBytes != noOfBytesRead) {
                // TODO This is and error..
            }

            attrDataMap.put(attrKind, attrData);
        }
        return attrDataMap;
    }

    /**
     * Set parameter symbols to the invokable symbol.
     *
     * @param invokableSymbol Invokable symbol
     * @param attrDataMap     Attribute data map
     * @throws IOException
     */
    private void setParamSymbols(BInvokableSymbol invokableSymbol, Map<AttributeInfo.Kind, byte[]> attrDataMap)
            throws IOException {

        if (!attrDataMap.containsKey(AttributeInfo.Kind.PARAMETERS_ATTRIBUTE) ||
                !attrDataMap.containsKey(AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE)) {
            return;
        }

        // Get parameter counts
        byte[] paramData = attrDataMap.get(AttributeInfo.Kind.PARAMETERS_ATTRIBUTE);
        DataInputStream paramDataInStream = new DataInputStream(new ByteArrayInputStream(paramData));
        int requiredParamCount = paramDataInStream.readInt();
        int defaultableParamCount = paramDataInStream.readInt();
        int restParamCount = paramDataInStream.readInt();

        // Get var names and create var symbols
        byte[] localVarData = attrDataMap.get(AttributeInfo.Kind.LOCAL_VARIABLES_ATTRIBUTE);
        DataInputStream localVarDataInStream = new DataInputStream(new ByteArrayInputStream(localVarData));
        localVarDataInStream.readShort();
        BInvokableType funcType = (BInvokableType) invokableSymbol.type;
        if (Symbols.isFlagOn(invokableSymbol.flags, Flags.ATTACHED)) {
            //remove first variable name
            getVarName(localVarDataInStream);
        }
        for (int i = 0; i < requiredParamCount; i++) {
            String varName = getVarName(localVarDataInStream);
            BVarSymbol varSymbol = new BVarSymbol(0, names.fromString(varName), this.env.pkgSymbol.pkgID,
                    funcType.paramTypes.get(i), invokableSymbol);
            invokableSymbol.params.add(varSymbol);
        }

        for (int i = requiredParamCount; i < requiredParamCount + defaultableParamCount; i++) {
            String varName = getVarName(localVarDataInStream);
            BVarSymbol varSymbol = new BVarSymbol(0, names.fromString(varName), this.env.pkgSymbol.pkgID,
                    funcType.paramTypes.get(i), invokableSymbol);
            varSymbol.defaultableParam = true;
            invokableSymbol.params.add(varSymbol);
        }

        if (restParamCount == 1) {
            String varName = getVarName(localVarDataInStream);
            BVarSymbol varSymbol = new BVarSymbol(0, names.fromString(varName), this.env.pkgSymbol.pkgID,
                    funcType.paramTypes.get(requiredParamCount + defaultableParamCount), invokableSymbol);
            invokableSymbol.restParam = varSymbol;
        }
    }

    private DefaultValueLiteral getDefaultValue(DataInputStream dataInStream)
            throws IOException {
        String typeDesc = getUTF8CPEntryValue(dataInStream);

        int valueCPIndex;
        switch (typeDesc) {
            case TypeDescriptor.SIG_BOOLEAN:
                return new DefaultValueLiteral(dataInStream.readBoolean(), TypeTags.BOOLEAN);
            case TypeDescriptor.SIG_INT:
                valueCPIndex = dataInStream.readInt();
                IntegerCPEntry integerCPEntry = (IntegerCPEntry) this.env.constantPool[valueCPIndex];
                return new DefaultValueLiteral(integerCPEntry.getValue(), TypeTags.INT);
            case TypeDescriptor.SIG_BYTE:
                valueCPIndex = dataInStream.readInt();
                IntegerCPEntry byteEntry = (IntegerCPEntry) this.env.constantPool[valueCPIndex];
                return new DefaultValueLiteral(byteEntry.getValue(), TypeTags.BYTE);
            case TypeDescriptor.SIG_FLOAT:
                valueCPIndex = dataInStream.readInt();
                FloatCPEntry floatCPEntry = (FloatCPEntry) this.env.constantPool[valueCPIndex];
                return new DefaultValueLiteral(floatCPEntry.getValue(), TypeTags.FLOAT);
            case TypeDescriptor.SIG_DECIMAL:
                valueCPIndex = dataInStream.readInt();
                UTF8CPEntry decimalEntry = (UTF8CPEntry) this.env.constantPool[valueCPIndex];
                return new DefaultValueLiteral(decimalEntry.getValue(), TypeTags.DECIMAL);
            case TypeDescriptor.SIG_STRING:
                valueCPIndex = dataInStream.readInt();
                UTF8CPEntry stringCPEntry = (UTF8CPEntry) this.env.constantPool[valueCPIndex];
                return new DefaultValueLiteral(stringCPEntry.getValue(), TypeTags.STRING);
            case TypeDescriptor.SIG_NULL:
                break;
            default:
                throw new RuntimeException("unknown default value type " + typeDesc);
        }
        return null;
    }

    /**
     * Set taint table to the invokable symbol.
     *
     * @param invokableSymbol Invokable symbol
     * @param attrDataMap     Attribute data map
     * @throws IOException
     */
    private void setTaintTable(BInvokableSymbol invokableSymbol, Map<AttributeInfo.Kind, byte[]> attrDataMap)
            throws IOException {

        if (!attrDataMap.containsKey(Kind.TAINT_TABLE) ||
                !attrDataMap.containsKey(AttributeInfo.Kind.TAINT_TABLE)) {
            return;
        }

        // Get taint table dimensions
        byte[] taintTableBytes = attrDataMap.get(AttributeInfo.Kind.TAINT_TABLE);
        DataInputStream taintTableDataInStream = new DataInputStream(new ByteArrayInputStream(taintTableBytes));
        int rowCount = taintTableDataInStream.readShort();
        int columnCount = taintTableDataInStream.readShort();

        // Extract and set taint table to the symbol
        invokableSymbol.taintTable = new HashMap<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            int paramIndex = taintTableDataInStream.readShort();
            TaintRecord.TaintedStatus returnTaintedStatus =
                    convertByteToTaintedStatus(taintTableDataInStream.readByte());
            List<TaintRecord.TaintedStatus> parameterTaintedStatusList = new ArrayList<>();
            for (int columnIndex = 1; columnIndex < columnCount; columnIndex++) {
                parameterTaintedStatusList.add(convertByteToTaintedStatus(taintTableDataInStream.readByte()));
            }
            TaintRecord taintRecord = new TaintRecord(returnTaintedStatus, parameterTaintedStatusList);
            invokableSymbol.taintTable.put(paramIndex, taintRecord);
        }
    }

    private TaintRecord.TaintedStatus convertByteToTaintedStatus(byte readByte) {
        return EnumSet.allOf(TaintRecord.TaintedStatus.class).stream()
                .filter(taintedStatus -> readByte == taintedStatus.getByteValue()).findFirst().get();
    }

    private void setDocumentation(BSymbol symbol, Map<AttributeInfo.Kind, byte[]> attrDataMap) throws IOException {
        if (!attrDataMap.containsKey(Kind.DOCUMENT_ATTACHMENT_ATTRIBUTE)) {
            return;
        }

        byte[] documentationBytes = attrDataMap.get(AttributeInfo.Kind.DOCUMENT_ATTACHMENT_ATTRIBUTE);
        DataInputStream documentDataStream = new DataInputStream(new ByteArrayInputStream(documentationBytes));

        String docDesc = getUTF8CPEntryValue(documentDataStream);

        MarkdownDocAttachment docAttachment = new MarkdownDocAttachment();
        docAttachment.description = docDesc;

        int noOfParams = documentDataStream.readShort();
        for (int i = 0; i < noOfParams; i++) {
            String name = getUTF8CPEntryValue(documentDataStream);
            String paramDesc = getUTF8CPEntryValue(documentDataStream);
            MarkdownDocAttachment.Parameter parameter = new MarkdownDocAttachment.Parameter(name, paramDesc);
            docAttachment.parameters.add(parameter);
        }

        boolean isReturnDocDescriptionAvailable = documentDataStream.readBoolean();
        if (isReturnDocDescriptionAvailable) {
            docAttachment.returnValueDescription = getUTF8CPEntryValue(documentDataStream);
        }

        symbol.markdownDocumentation = docAttachment;
    }

    private String getVarName(DataInputStream dataInStream) throws IOException {
        String varName = getUTF8CPEntryValue(dataInStream);
        // read variable index
        dataInStream.readInt();
        dataInStream.readInt();
        dataInStream.readInt();
        dataInStream.readInt();
        // Read and ignore identifier kind flag
        dataInStream.readBoolean();

        int attchmntIndexesLength = dataInStream.readShort();
        for (int i = 0; i < attchmntIndexesLength; i++) {
            dataInStream.readInt();
        }

        return varName;
    }

    private Object getObjectFieldDefaultValue(byte[] defaultValueAttrData) throws IOException {
        try (DataInputStream dataInStream = new DataInputStream(
                new ByteArrayInputStream(defaultValueAttrData))) {
            String typeDesc = getUTF8CPEntryValue(dataInStream);
            switch (typeDesc) {
                case TypeDescriptor.SIG_INT:
                    return dataInStream.readInt();
                case TypeDescriptor.SIG_FLOAT:
                    return dataInStream.readFloat();
                case TypeDescriptor.SIG_DECIMAL:
                    return dataInStream.readUTF();
                case TypeDescriptor.SIG_BOOLEAN:
                    return dataInStream.readBoolean();
                case TypeDescriptor.SIG_STRING:
                    return getUTF8CPEntryValue(dataInStream);
                default:
                    throw new BLangCompilerException("unknown default value type " + typeDesc);
            }
        }
    }


    // private utility methods

    private String getUTF8CPEntryValue(DataInputStream dataInStream) throws IOException {
        int pkgNameCPIndex = dataInStream.readInt();
        UTF8CPEntry pkgNameCPEntry = (UTF8CPEntry) this.env.constantPool[pkgNameCPIndex];
        return pkgNameCPEntry.getValue();
    }

    private PackageID createPackageID(String orgName, String pkgName, String pkgVersion) {
        if (orgName == null || orgName.isEmpty()) {
            throw new BLangCompilerException("invalid module name '" + pkgName + "' in compiled package file");
        }

        return new PackageID(names.fromString(orgName),
                names.fromString(pkgName),
                names.fromString(pkgVersion));
    }

    private BInvokableType createInvokableType(String sig) {
        char[] chars = sig.toCharArray();
        Stack<BType> typeStack = new Stack<>();
        this.typeSigReader.createFunctionType(new CompilerTypeCreator(), chars, 0, typeStack);
        return (BInvokableType) typeStack.pop();
    }

    private BPackageSymbol lookupPackageSymbol(String packagePath) {
        //TODO below is a temporary fix, this needs to be removed later.
        PackageID pkgID = getPackageID(packagePath);
        if (pkgID.equals(env.pkgSymbol.pkgID)) {
            return env.pkgSymbol;
        }

        BSymbol symbol = lookupMemberSymbol(this.env.pkgSymbol.scope, pkgID.name, SymTag.PACKAGE);
        if (symbol == this.symTable.notFoundSymbol && pkgID.orgName.equals(Names.BUILTIN_ORG)) {
            symbol = this.packageLoader.loadPackageSymbol(pkgID, this.env.pkgSymbol.pkgID, this.env.repoHierarchy);
            if (symbol == null) {
                throw new BLangCompilerException("unknown imported module: " + pkgID.name);
            }
        }

        return (BPackageSymbol) symbol;
    }

    private PackageID getPackageID(String packagePath) {
        String[] orgNameParts = packagePath.split(Names.ORG_NAME_SEPARATOR.value);
        String[] pkgNameParts = orgNameParts[1].split(":");
        String version = pkgNameParts.length == 2 ? pkgNameParts[1] : Names.EMPTY.value;
        return createPackageID(orgNameParts[0], pkgNameParts[0], version);
    }

    private BSymbol lookupMemberSymbol(Scope scope, Name name, int expSymTag) {
        Scope.ScopeEntry entry = scope.lookup(name);
        while (entry != NOT_FOUND_ENTRY) {
            if ((entry.symbol.tag & expSymTag) != expSymTag) {
                entry = entry.next;
            } else {
                return entry.symbol;
            }
        }

        return symTable.notFoundSymbol;
    }

    private BType lookupUserDefinedType(BPackageSymbol packageSymbol, String typeName) {
        BSymbol typeSymbol = lookupMemberSymbol(packageSymbol.scope, names.fromString(typeName), SymTag.TYPE);
        if (typeSymbol == this.symTable.notFoundSymbol) {
            throw new BLangCompilerException("unknown type name: " + typeName);
        }

        return typeSymbol.type;
    }

    private BType getBuiltinRefTypeFromName(String typeName) {
        BSymbol typeSymbol = lookupMemberSymbol(this.symTable.rootScope, names.fromString(typeName), SymTag.TYPE);
        if (typeSymbol == this.symTable.notFoundSymbol) {
            throw new BLangCompilerException("unknown type name: " + typeName);
        }

        return typeSymbol.type;
    }

    private void assignInitFunctions() {
        BPackageSymbol pkgSymbol = this.env.pkgSymbol;
        PackageID pkgId = pkgSymbol.pkgID;
        Name initFuncName = names.merge(names.fromString(pkgId.toString()), Names.INIT_FUNCTION_SUFFIX);
        BSymbol initFuncSymbol = lookupMemberSymbol(pkgSymbol.scope, initFuncName, SymTag.FUNCTION);
        pkgSymbol.initFunctionSymbol = (BInvokableSymbol) initFuncSymbol;

        Name startFuncName = names.merge(names.fromString(pkgId.toString()), Names.START_FUNCTION_SUFFIX);
        BSymbol startFuncSymbol = lookupMemberSymbol(pkgSymbol.scope, startFuncName, SymTag.FUNCTION);
        pkgSymbol.startFunctionSymbol = (BInvokableSymbol) startFuncSymbol;

        Name stopFuncName = names.merge(names.fromString(pkgId.toString()), Names.STOP_FUNCTION_SUFFIX);
        BSymbol stopFuncSymbol = lookupMemberSymbol(pkgSymbol.scope, stopFuncName, SymTag.FUNCTION);
        pkgSymbol.stopFunctionSymbol = (BInvokableSymbol) stopFuncSymbol;
    }

    private void resolveTypes() {
        for (UnresolvedType unresolvedType : this.env.unresolvedTypes) {
            BType type = getBTypeFromDescriptor(unresolvedType.typeSig);
            unresolvedType.completer.accept(type);
        }
    }

    private void updateUnresolvedConstantReferences() {
        for (Map.Entry<BLangConstRef, MapCPEntry> entry : this.env.unresolvedConstReferences.entrySet()) {
            BLangConstRef ref = entry.getKey();
            MapCPEntry cpEntry = entry.getValue();
            // Set the symbol of the constant reference.
            ref.symbol = cpEntry.getConstantSymbol();
        }
        // Clear the map.
        this.env.unresolvedConstReferences.clear();
    }

    private BType getBTypeFromDescriptor(String typeSig) {
        return this.typeSigReader.getBTypeFromDescriptor(new CompilerTypeCreator(), typeSig);
    }

    private BLangLiteral createLiteralBasedOnDescriptor(String typeSig) {
        BType type = getBTypeFromDescriptor(typeSig);
        NodeKind nodeKind = type.tag <= TypeTags.DECIMAL ? NodeKind.NUMERIC_LITERAL : NodeKind.LITERAL;
        return nodeKind == NodeKind.LITERAL ? (BLangLiteral) TreeBuilder.createLiteralExpression() :
                (BLangLiteral) TreeBuilder.createNumericLiteralExpression();
    }

    /**
     * This class holds compiled package specific information during the symbol enter phase of the compiled package.
     *
     * @since 0.970.0
     */
    private static class CompiledPackageSymbolEnv {
        PackageID requestedPackageId;
        RepoHierarchy repoHierarchy;
        BPackageSymbol pkgSymbol;
        ConstantPoolEntry[] constantPool;
        List<UnresolvedType> unresolvedTypes;
        Map<BLangConstRef, MapCPEntry> unresolvedConstReferences;

        CompiledPackageSymbolEnv() {
            this.unresolvedTypes = new ArrayList<>();
            this.unresolvedConstReferences = new HashMap<>();
        }
    }

    private static class UnresolvedType {
        String typeSig;
        Consumer<BType> completer;

        UnresolvedType(String typeSig, Consumer<BType> completer) {
            this.typeSig = typeSig;
            this.completer = completer;
        }
    }

    /**
     * Create types for compiler phases.
     *
     * @since 0.975.0
     */
    private class CompilerTypeCreator implements TypeCreator<BType> {

        @Override
        public BType getBasicType(char typeChar) {
            switch (typeChar) {
                case 'I':
                    return symTable.intType;
                case 'W':
                    return symTable.byteType;
                case 'F':
                    return symTable.floatType;
                case 'L':
                    return symTable.decimalType;
                case 'S':
                    return symTable.stringType;
                case 'B':
                    return symTable.booleanType;
                case 'Y':
                    return symTable.typeDesc;
                case 'A':
                    return symTable.anyType;
                case 'N':
                    return symTable.nilType;
                case 'K':
                    return symTable.anydataType;
                default:
                    throw new IllegalArgumentException("unsupported basic type char: " + typeChar);
            }
        }

        @Override
        public BType getBuiltinRefType(String typeName) {
            return getBuiltinRefTypeFromName(typeName);
        }

        @Override
        public BType getRefType(char typeChar, String pkgPath, String typeName) {
            if (typeName.isEmpty()) {
                return null;
            }

            BPackageSymbol pkgSymbol;
            if (pkgPath != null) {
                pkgSymbol = lookupPackageSymbol(pkgPath);
            } else {
                pkgSymbol = env.pkgSymbol;
            }

            switch (typeChar) {
                case 'X':
                    return symTable.anyServiceType;
                default:
                    return lookupUserDefinedType(pkgSymbol, typeName);
            }
        }

        @Override
        public BType getConstrainedType(char typeChar, BType constraint) {
            switch (typeChar) {
                case 'D':
                    if (constraint == null) {
                        return symTable.tableType;
                    }
                    return new BTableType(TypeTags.TABLE, constraint, symTable.tableType.tsymbol);
                case 'M':
                    if (constraint == null || constraint == symTable.anyType) {
                        return symTable.mapType;
                    }
                    return new BMapType(TypeTags.MAP, constraint, symTable.mapType.tsymbol);
                case 'H':
                    return new BStreamType(TypeTags.STREAM, constraint, symTable.streamType.tsymbol);
                case 'G':
                case 'T':
                case 'X':
                default:
                    return constraint;
            }
        }

        @Override
        public BType getArrayType(BType elementType, int size) {
            BTypeSymbol arrayTypeSymbol = Symbols.createTypeSymbol(SymTag.ARRAY_TYPE, Flags.asMask(EnumSet
                    .of(Flag.PUBLIC)), Names.EMPTY, env.pkgSymbol.pkgID, null, env.pkgSymbol.owner);
            return size == -1 ? new BArrayType(elementType, arrayTypeSymbol, size, BArrayState.UNSEALED) :
                    new BArrayType(elementType, arrayTypeSymbol, size, BArrayState.CLOSED_SEALED);
        }

        @Override
        public BType getCollectionType(char typeChar, List<BType> memberTypes) {

            switch (typeChar) {
                case 'O':
                    BTypeSymbol unionTypeSymbol = Symbols.createTypeSymbol(SymTag.UNION_TYPE, Flags.asMask(EnumSet
                            .of(Flag.PUBLIC)), Names.EMPTY, env.pkgSymbol.pkgID, null, env.pkgSymbol.owner);
                    return BUnionType.create(unionTypeSymbol, new LinkedHashSet<>(memberTypes));
                case 'P':
                    BTypeSymbol tupleTypeSymbol = Symbols.createTypeSymbol(SymTag.TUPLE_TYPE, Flags.asMask(EnumSet
                            .of(Flag.PUBLIC)), Names.EMPTY, env.pkgSymbol.pkgID, null, env.pkgSymbol.owner);
                    return new BTupleType(tupleTypeSymbol, memberTypes);
                default:
                    throw new IllegalArgumentException("unsupported collection type char: " + typeChar);
            }
        }

        @Override
        public BType getFunctionType(List<BType> funcParams, BType retType) {
            if (retType == null) {
                retType = symTable.nilType;
            }
            BTypeSymbol tsymbol = Symbols.createTypeSymbol(SymTag.FUNCTION_TYPE, Flags.asMask(EnumSet.of(Flag.PUBLIC)),
                                                           Names.EMPTY, env.pkgSymbol.pkgID, null,
                                                           env.pkgSymbol.owner);
            return new BInvokableType(funcParams, retType, tsymbol);
        }

        @Override
        public BType getErrorType(BType reasonType, BType detailsType) {
            if (reasonType == symTable.stringType && detailsType == symTable.detailType) {
                return symTable.errorType;
            }
            BTypeSymbol errorSymbol = new BErrorTypeSymbol(SymTag.ERROR, Flags.PUBLIC, Names.EMPTY,
                    env.pkgSymbol.pkgID, null, env.pkgSymbol.owner);
            BErrorType errorType = new BErrorType(errorSymbol, reasonType, detailsType);
            errorSymbol.type = errorType;
            return errorType;
        }
    }
}
