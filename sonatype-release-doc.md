# Release to Maven Central

1. Create a [ticket with Sonatype](http://central.sonatype.org/pages/ossrh-guide.html)  
(This has to be done by our maintenance department once per project).

2. Install a [gpg client](http://central.sonatype.org/pages/apache-maven.html#other-prerequisites) to sign the deployment artifacts  
(This step has obviously to be done once per client).

3. Prepare the release:  
`$ mvn release:prepare`

4. Perform the release:  
`$ mvn release:perform`

5. Verify the release on Maven central:
- Navigate to [oss.sonatype.org](https://oss.sonatype.org/)
- Log in
- Go to 'Staging Repositories'
- Search on '42'
- Select the artifact
- Press 'release'
