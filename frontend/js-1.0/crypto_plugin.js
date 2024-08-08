let canAsync;
window.certList = function run() {
    // Проверка на наличие cadesplugin
    if (!cadesplugin) {
        alert("cadesplugin не доступен. Убедитесь, что плагин установлен.");
        return;
    }

    canAsync = !!cadesplugin.CreateObjectAsync;
    console.log("canAsync " + canAsync);

    let oStore, ret;
    if (canAsync) {
        return cadesplugin.then(function () {
            return cadesplugin.CreateObjectAsync("CAPICOM.Store");
        }).then(store => {
            oStore = store;
            return oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE,
                cadesplugin.CAPICOM_MY_STORE,
                cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);
        }).then(() => {
            console.log("type of ", typeof oStore.Open);
            ret = fetchCertsFromStore(oStore);
            console.log("ret: ", ret);
            return fetchCertsFromStore(oStore);
        });
    }

    #certificatesSelect
    return ret;
}

function fetchCertsFromStore(oStore, skipIds = []) {
    if (canAsync) {
        let oCertificates;
        return oStore.Certificates.then(certificates => {
            oCertificates = certificates;
            return certificates.Count;
        }).then(count => {
            const certs = [];
            for (let i = 1; i <= count; i++) certs.push(oCertificates.Item(i));
            return Promise.all(certs);
        }).then(certificates => {
            const certs = [];
            for (let i in certificates) certs.push(
                certificates[i].SubjectName,
                certificates[i].Thumbprint,
                certificates[i].ValidFromDate,
                certificates[i].ValidToDate
            );
            return Promise.all(certs);
        }).then(data => {
            const certs = [];
            for (let i = 0; i < data.length; i += 4) {
                const id = data[i + 1];
                if (skipIds.indexOf(id) + 1) break;
                const oDN = string2dn(data[i]);
                certs.push({
                    id,
                    name: formatCertificateName(oDN),
                    subject: oDN,
                    validFrom: new Date(data[i + 2]),
                    validTo: new Date(data[i + 3])
                });
            }
            return certs;
        });
    } else {
        const oCertificates = oStore.Certificates;
        const certs = [];
        for (let i = 1; i <= oCertificates.Count; i++) {
            const oCertificate = oCertificates.Item(i);
            const id = oCertificate.Thumbprint;
            if (skipIds.indexOf(id) + 1) break;
            const oDN = string2dn(oCertificate.SubjectName);
            certs.push({
                id,
                name: formatCertificateName(oDN),
                subject: oDN,
                validFrom: new Date(oCertificate.ValidFromDate),
                validTo: new Date(oCertificate.ValidToDate)
            });
        }
        return certs;
    }
}

/**
 * Разобрать субъект в объект DN
 * @param {string} subjectName
 * @returns {DN}
 */
function string2dn(subjectName) {
    const dn = new DN;
    let pairs = subjectName.match(/([а-яёА-ЯЁa-zA-Z0-9\.\s]+)=(?:("[^"]+?")|(.+?))(?:,|$)/g);
    if (pairs) pairs = pairs.map(el => el.replace(/,$/, ''));
    else pairs = []; //todo: return null?
    pairs.forEach(pair => {
        const d = pair.match(/([^=]+)=(.*)/);
        if (d && d.length === 3) {
            const rdn = d[1].trim().replace(/^OID\./, '');
            dn[rdn] = d[2].trim()
                .replace(/^"(.*)"$/, '$1')
                .replace(/""/g, '"');
        }
    });
    return convertDN(dn);
}

function DN(){}

DN.prototype.toString = function () {
    let ret = '';
    for (let i in this) {
        if (this.hasOwnProperty(i)) {
            ret += i + '="' + this[i].replace(/"/g, '\'') + '", ';
        }
    }
    return ret;
};

function convertDN(dn) {
    const result = new DN;
    for (const field of Object.keys(dn)) {
        const oid = oids.find(item => item.oid == field || item.full == field);
        if (oid) {
            result[oid.short] = dn[field];
        }
        else {
            result[field] = dn[field];
        }
    }
    return result;
}

const oids = [
    { oid: '1.2.643.3.131.1.1',    short: 'INN',    full: 'ИНН' },
    { oid: '1.2.643.100.4',        short: 'INNLE',  full: 'ИНН ЮЛ' },
    { oid: '1.2.643.100.1',        short: 'OGRN',   full: 'ОГРН' },
    { oid: '1.2.643.100.5',        short: 'OGRNIP', full: 'ОГРНИП' },
    { oid: '1.2.643.100.3',        short: 'SNILS',  full: 'СНИЛС' },
    { oid: '1.2.840.113549.1.9.1', short: 'E',      full: 'emailAddress' },
    { oid: '2.5.4.3',              short: 'CN',     full: 'commonName' },
    { oid: '2.5.4.4',              short: 'SN',     full: 'surname' },
    { oid: '2.5.4.42',             short: 'G',      full: 'givenName' },
    { oid: '2.5.4.6',              short: 'C',      full: 'countryName' },
    { oid: '2.5.4.7',              short: 'L',      full: 'localityName' },
    { oid: '2.5.4.8',              short: 'S',      full: 'stateOrProvinceName' },
    { oid: '2.5.4.9',              short: 'STREET', full: 'streetAddress' },
    { oid: '2.5.4.10',             short: 'O',      full: 'organizationName' },
    { oid: '2.5.4.11',             short: 'OU',     full: 'organizationalUnitName' },
    { oid: '2.5.4.12',             short: 'T',      full: 'title' },
//  { oid: '2.5.4.16',             short: '?',      full: 'postalAddress' },
];

/**
 * Получить название сертификата
 * @param {DN} o объект, включающий в себя значения субъекта сертификата
 * @see convertDN
 * @returns {String}
 */
function formatCertificateName(o) {
    return '' + o['CN']
        + (o['INNLE'] ? '; ИНН ЮЛ ' + o['INNLE'] : '')
        + (o['INN'] ? '; ИНН ' + o['INN'] : '')
        + (o['SNILS'] ? '; СНИЛС ' + o['SNILS'] : '');
}

window.loadCertificates = async function loadCertificates() {
    try {
        // Проверка на наличие cadesplugin
        if (!cadesplugin) {
            alert("cadesplugin не доступен. Убедитесь, что плагин установлен.");
            return;
        }

        //проверить async для cadesplugin
        const canAsync = !!cadesplugin.CreateObjectAsync;
        console.log("canAsync " + canAsync);

        // Инициализация cadesplugin
        const oStore = await cadesplugin.CreateObjectAsync("CAPICOM.Store");
        console.log("store ", oStore);

        //const signerOptions = cadesplugin.CAPICOM_CERTIFICATE_INCLUDE_WHOLE_CHAIN;
        const signerOptions = cadesplugin.CAPICOM_CERTIFICATE_INCLUDE_END_ENTITY_ONLY;
        console.log("signerOptions ", signerOptions);

        var oAbout = cadesplugin.CreateObjectAsync("CAdESCOM.About");
        console.log("oAbout ", oAbout);

        var pluginVersion = await oAbout.then((about) => {
            return about.Version;
        });
        console.log("pluginVersion ", pluginVersion);
        // <div id="indicator">Загрузка плагина...</div>
        if (pluginVersion) {
            var indicator = document.getElementById("indicator");
            indicator.style.backgroundColor = "rgba(144, 238, 144, 0.7)";
            indicator.innerText = 'Версия плагина: ' + pluginVersion;
        }

        var awaitCert = cadesplugin.CreateObjectAsync("CAdESCOM.Certificate");
        console.log("await cert ", awaitCert);
        var certificates = await awaitCert.then((r_cert) => {
            return r_cert;
        });
        console.log("cert ", certificates);

        // var rawCerts = oStore.Certificates;
        // console.log("rawCerts", rawCerts);

        // function Common_CheckForPlugIn() {
        cadesplugin.set_log_level(cadesplugin.LOG_LEVEL_DEBUG);
        if (canAsync) {
            debugger;
            include_async_code().then(function () {
                return CheckForPlugIn_Async();
            });
        } else {
            return CheckForPlugIn_NPAPI();
        }

        // Получение хранилища сертификатов
        // await store.Open(2, "", 0); // 2 - CAPICOM_CURRENT_USER_STORE

        // Получение сертификатов
        // const certificates = await store.Certificates;

        // Получение элемента select
        const selectBox = document.getElementById("certificatesSelect");
        // selectBox.innerHTML = ""; // Очистка предыдущих значений

        console.log("certificates.Count ", certificates.Count);
        // Заполнение select с сертификатами
        for (let i = 1; i <= certificates.Count; i++) {
            const cert = await certificates.Item(i);
            console.log("cert ", i, cert)
            const option = document.createElement("option");
            option.value = await cert.Thumbprint; // Используем отпечаток сертификата как значение
            option.text = await cert.SubjectName; // Используем имя субъекта как текст
            console.log("value ", value, "text ", text);
            selectBox.appendChild(option);
        }
    } catch (error) {
        console.error("Ошибка при загрузке сертификатов:", error);
        alert("Ошибка при загрузке сертификатов: " + error.message);
    }
}

var async_code_included = 0;
var async_Promise;
var async_resolve;

function include_async_code() {
    if (async_code_included) {
        return async_Promise;
    }
    var fileref = document.createElement('script');
    fileref.setAttribute("type", "text/javascript");
    fileref.setAttribute("src", "../script/async_code.js?v=271851");
    document.getElementsByTagName("head")[0].appendChild(fileref);
    async_Promise = new Promise(function (resolve, reject) {
        async_resolve = resolve;
    });
    async_code_included = 1;
    return async_Promise;
}