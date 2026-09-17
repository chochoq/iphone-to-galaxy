// Isolated test APK using unmodified, released RecyclerView 1.4.0. Not shipped in the product.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
if(!process.argv[2])throw Error('Pass a fresh output directory');
const out=path.resolve(process.argv[2]);
if(fs.existsSync(out))throw Error('Output already exists; preserve it and pass a fresh path');
for(const folder of ['deps','classes','dex','gen'])fs.mkdirSync(path.join(out,folder),{recursive:true});
const sdk=process.env.ANDROID_SDK_ROOT;
if(!sdk)throw Error('Set ANDROID_SDK_ROOT to the Android SDK directory');
const bt=sdk+'/build-tools/36.0.0',jar=sdk+'/platforms/android-36/android.jar';
const run=(cmd,args,options={})=>execFileSync(cmd,args,{stdio:'inherit',...options});
const files=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(path.join(p,e.name)):[path.join(p,e.name)]);
const google='https://dl.google.com/dl/android/maven2/',central='https://repo.maven.apache.org/maven2/';
const aars=[
 ['androidx/recyclerview/recyclerview/1.4.0/recyclerview-1.4.0','androidx.recyclerview'],
 ['androidx/core/core/1.13.1/core-1.13.1','androidx.core'],
 ['androidx/core/core-ktx/1.13.1/core-ktx-1.13.1','androidx.core.ktx'],
 ['androidx/customview/customview/1.0.0/customview-1.0.0','androidx.customview'],
 ['androidx/customview/customview-poolingcontainer/1.0.0/customview-poolingcontainer-1.0.0','androidx.customview.poolingcontainer'],
];
const jars=[];const resources=[];
for(const [artifact,pkg] of aars){
 const name=artifact.split('/').at(-1),dest=out+'/deps/'+name;fs.mkdirSync(dest);
 run('curl',['--fail','--silent','--show-error','--location',google+artifact+'.aar','-o',dest+'.aar']);
 run('unzip',['-q',dest+'.aar','-d',dest]);jars.push(dest+'/classes.jar');
 if(fs.existsSync(dest+'/res')){run(bt+'/aapt2',['compile','--dir',dest+'/res','-o',dest+'/res.zip']);resources.push(dest+'/res.zip');}
}
for(const [base,artifact] of [
 [google,'androidx/collection/collection-jvm/1.4.2/collection-jvm-1.4.2'],
 [google,'androidx/annotation/annotation-jvm/1.8.1/annotation-jvm-1.8.1'],
 [central,'org/jetbrains/kotlin/kotlin-stdlib/1.9.24/kotlin-stdlib-1.9.24'],
]){const dest=out+'/deps/'+artifact.split('/').at(-1)+'.jar';run('curl',['--fail','--silent','--show-error','--location',base+artifact+'.jar','-o',dest]);jars.push(dest);}
run(bt+'/aapt2',['link','-o',out+'/unsigned.apk','-I',jar,'--manifest',root+'/tests/scroll-native/AndroidManifest.xml',
 '--java',out+'/gen','--extra-packages',aars.map(v=>v[1]).join(':'),'--auto-add-overlay',...resources.flatMap(v=>['-R',v])]);
run('javac',['-encoding','UTF-8','-source','8','-target','8','-classpath',[jar,...jars].join(':'),'-d',out+'/classes',
 ...files(root+'/tests/scroll-native').filter(p=>p.endsWith('.java')),...files(out+'/gen').filter(p=>p.endsWith('.java'))]);
run(bt+'/d8',['--lib',jar,'--min-api','31','--output',out+'/dex',...files(out+'/classes'),...jars]);
run('zip',['-q',out+'/unsigned.apk','classes.dex'],{cwd:out+'/dex'});
run(bt+'/zipalign',['4',out+'/unsigned.apk',out+'/aligned.apk']);
const key=process.argv[3]?path.resolve(process.argv[3]):out+'/test.jks';
if(!fs.existsSync(key))run('keytool',['-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','test','-dname','CN=Scroll006 isolated test','-keyalg','RSA','-validity','30','-noprompt']);
run(bt+'/apksigner',['sign','--ks',key,'--ks-pass','pass:android','--out',out+'/scroll-test.apk',out+'/aligned.apk']);
console.log(out+'/scroll-test.apk');
