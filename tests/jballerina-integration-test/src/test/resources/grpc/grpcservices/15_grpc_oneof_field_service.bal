// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/grpc;
import ballerina/io;

@grpc:ServiceDescriptor {
    descriptor: ROOT_DESCRIPTOR_15,
    descMap: getDescriptorMap15()
}
service OneofFieldService on new grpc:Listener(9105) {

    resource function hello(grpc:Caller caller, Request1 value) {
        io:println("################################################################");
        io:println(value);
        string request = "";
        if (value.name is Request1_FirstName) {
            var conv = Request1_FirstName.convert(value.name);
            if (conv is Request1_FirstName) {
                request = conv.first_name;
            }
        } else {
            Request1_LastName|error conv = Request1_LastName.convert(value.name);
            if (conv is Request1_LastName) {
                request = conv.last_name;
            }
        }
        Response1 response = {message: "Hello " + request};
        io:println(response);
        checkpanic caller->send(response);
        checkpanic caller->complete();
    }

    resource function testOneofField(grpc:Caller caller, ZZZ req) {
        checkpanic caller->send(req);
        checkpanic caller->complete();
    }
}

type Request1 record {
    Other other;
    Name name;
};

public type Other Request1_Age|Request1_Address|Request1_Married;

type Request1_Age record {
    int age;
};

type Request1_Address record {
    Address1 address;
};

type Request1_Married record {
    boolean married;
};

public type Name Request1_FirstName|Request1_LastName;

type Request1_FirstName record {
    string first_name;
};

type Request1_LastName record {
    string last_name;
};

type Address1 record {
    Code code;
};

public type Code Address1_HouseNumber|Address1_StreetNumber;

type Address1_HouseNumber record {
    int house_number;

};

type Address1_StreetNumber record {
    int street_number;
};

type Response1 record {
    string message;
};

type ZZZ record {
    float aa = 1.2345;
    float bb = 1.23;
    int cc = 10;
    int dd = 11;
    int ee = 12;
    int ff = 13;
    int gg = 14;
    boolean hh = true;
    string ii = "Test";
    AAA jj = {};
    byte[] kk = [24, 25];
    Value value;
};

public type Value ZZZ_OneA|ZZZ_OneB|ZZZ_OneC|ZZZ_OneD|ZZZ_OneE|ZZZ_OneF|ZZZ_OneG|ZZZ_OneH|ZZZ_OneI|ZZZ_OneJ|ZZZ_OneK;

type ZZZ_OneA record {
    float one_a;
};

type ZZZ_OneB record {
    float one_b;
};

type ZZZ_OneC record {
    int one_c;
};

type ZZZ_OneD record {
    int one_d;
};

type ZZZ_OneE record {
    int one_e;
};

type ZZZ_OneF record {
    int one_f;
};

type ZZZ_OneG record {
    int one_g;
};

type ZZZ_OneH record {
    boolean one_h;
};

type ZZZ_OneI record {
    string one_i;
};

type ZZZ_OneJ record {
    AAA one_j;
};

type ZZZ_OneK record {
    byte[] one_k;
};

type AAA record {
    string aaa = "aaa";
};

const string ROOT_DESCRIPTOR_15 =
"0A196F6E656F665F6669656C645F736572766963652E70726F746F120C67727063736572766963657322BF010A085265717565737431121F0A0A66697273745F6E616D651801200128094800520966697273744E616D65121D0A096C6173745F6E616D65180220012809480052086C6173744E616D6512120A036167651803200128054801520361676512320A076164647265737318042001280B32162E6772706373657276696365732E41646472657373314801520761646472657373121A0A076D617272696564180520012808480152076D61727269656442060A046E616D6542070A056F74686572225E0A08416464726573733112230A0C686F7573655F6E756D6265721801200128034800520B686F7573654E756D62657212250A0D7374726565745F6E756D6265721802200128074800520C7374726565744E756D62657242060A04636F646522250A09526573706F6E73653112180A076D65737361676518012001280952076D65737361676522E1030A035A5A5A12150A056F6E655F61180120012801480052046F6E654112150A056F6E655F62180220012802480052046F6E654212150A056F6E655F63180320012803480052046F6E654312150A056F6E655F64180420012804480052046F6E654412150A056F6E655F65180520012805480052046F6E654512150A056F6E655F66180620012806480052046F6E654612150A056F6E655F67180720012807480052046F6E654712150A056F6E655F68180820012808480052046F6E654812150A056F6E655F69180920012809480052046F6E654912280A056F6E655F6A180A2001280B32112E6772706373657276696365732E414141480052046F6E654A12150A056F6E655F6B180B2001280C480052046F6E654B120E0A026161180C2001280152026161120E0A026262180D2001280252026262120E0A026363180E2001280352026363120E0A026464180F2001280452026464120E0A02656518102001280552026565120E0A02666618112001280652026666120E0A02676718122001280752026767120E0A02686818132001280852026868120E0A0269691814200128095202696912210A026A6A18152001280B32112E6772706373657276696365732E41414152026A6A120E0A026B6B18162001280C52026B6B42070A0576616C756522170A0341414112100A0361616118012001280952036161613285010A114F6E656F664669656C645365727669636512380A0568656C6C6F12162E6772706373657276696365732E52657175657374311A172E6772706373657276696365732E526573706F6E73653112360A0E746573744F6E656F664669656C6412112E6772706373657276696365732E5A5A5A1A112E6772706373657276696365732E5A5A5A620670726F746F33";
function getDescriptorMap15() returns map<string> {
    return {
        "oneof_field_service.proto":"0A196F6E656F665F6669656C645F736572766963652E70726F746F120C67727063736572766963657322BF010A085265717565737431121F0A0A66697273745F6E616D651801200128094800520966697273744E616D65121D0A096C6173745F6E616D65180220012809480052086C6173744E616D6512120A036167651803200128054801520361676512320A076164647265737318042001280B32162E6772706373657276696365732E41646472657373314801520761646472657373121A0A076D617272696564180520012808480152076D61727269656442060A046E616D6542070A056F74686572225E0A08416464726573733112230A0C686F7573655F6E756D6265721801200128034800520B686F7573654E756D62657212250A0D7374726565745F6E756D6265721802200128074800520C7374726565744E756D62657242060A04636F646522250A09526573706F6E73653112180A076D65737361676518012001280952076D65737361676522E1030A035A5A5A12150A056F6E655F61180120012801480052046F6E654112150A056F6E655F62180220012802480052046F6E654212150A056F6E655F63180320012803480052046F6E654312150A056F6E655F64180420012804480052046F6E654412150A056F6E655F65180520012805480052046F6E654512150A056F6E655F66180620012806480052046F6E654612150A056F6E655F67180720012807480052046F6E654712150A056F6E655F68180820012808480052046F6E654812150A056F6E655F69180920012809480052046F6E654912280A056F6E655F6A180A2001280B32112E6772706373657276696365732E414141480052046F6E654A12150A056F6E655F6B180B2001280C480052046F6E654B120E0A026161180C2001280152026161120E0A026262180D2001280252026262120E0A026363180E2001280352026363120E0A026464180F2001280452026464120E0A02656518102001280552026565120E0A02666618112001280652026666120E0A02676718122001280752026767120E0A02686818132001280852026868120E0A0269691814200128095202696912210A026A6A18152001280B32112E6772706373657276696365732E41414152026A6A120E0A026B6B18162001280C52026B6B42070A0576616C756522170A0341414112100A0361616118012001280952036161613285010A114F6E656F664669656C645365727669636512380A0568656C6C6F12162E6772706373657276696365732E52657175657374311A172E6772706373657276696365732E526573706F6E73653112360A0E746573744F6E656F664669656C6412112E6772706373657276696365732E5A5A5A1A112E6772706373657276696365732E5A5A5A620670726F746F33"

    };
}
