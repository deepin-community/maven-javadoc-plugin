/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
int javaVersion = System.getProperty( "java.specification.version" ) as Integer
if ( javaVersion >= 11 ) {
    def index = new File( basedir, 'target/site/apidocs/index.html')

	assert index.text =~ /<a href="modulea\/module-summary\.html">modulea<\/a>/
	assert index.text =~ /<a href="moduleb\/module-summary\.html">moduleb<\/a>/
	
	assert new File( basedir, 'target/site/apidocs/modulea/module-summary.html').exists()
	assert new File( basedir, 'target/site/apidocs/moduleb/module-summary.html').exists()
} 
else {
    def overview = new File( basedir, 'target/site/apidocs/overview-summary.html')

	assert overview.text =~ /<a href="modulea-summary.html">modulea<\/a>/
	assert overview.text =~ /<a href="moduleb-summary.html">moduleb<\/a>/
	
	assert new File( basedir, 'target/site/apidocs/modulea-summary.html').exists()
	assert new File( basedir, 'target/site/apidocs/moduleb-summary.html').exists()
} 
