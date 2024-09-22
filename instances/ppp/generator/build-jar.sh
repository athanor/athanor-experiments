name='PPPGenerator'
rm $name.jar
mFile="manifest-${name}.txt"
echo "Main-Class: ${name}" > ${mFile}
javac ${name}.java
jar cvfm ${name}.jar ${mFile} ${name}.class
