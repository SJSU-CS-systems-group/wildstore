db = connect('mongodb://localhost:200/wildfire');
let allRecs = db.metadata.find({});
let myvarmap = new Map();
allRecs.forEach(rec => {
    for (let variable of rec.variables) {
        for (let attr of variable.attributeList) {
            if (attr.attributeName === "description") {
                myvarmap.set(variable.variableName, attr.value[0]);
            }
        }
    }
    for (let attr of rec.globalAttributes) {
        myattrset.add(attr.attributeName);
    }
})
let res = []
for(let key of myvarmap) {
    res.push({"label": key[0], "value": key[1]})
}
res.sort((a,b) => a.label.localeCompare(b.label));

let fs = require('node:fs');
fs.writeFile('variableDescriptions.json', JSON.stringify(res), err => {
  if (err) {
    console.error(err);
  } else {
    // file written successfully
    console.log('success')
  }
});

let resA = []
myattrset.forEach(key => {
    resA.push({"label": key, "value": key})
})
resA.sort((a,b) => a.label.localeCompare(b.label));

fs.writeFile('attributeDescriptions.json', JSON.stringify(resA), err => {
  if (err) {
    console.error(err);
  } else {
    // file written successfully
    console.log('success')
  }
});