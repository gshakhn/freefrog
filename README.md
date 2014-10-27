# freefrog

Exquisite Organization -- For Free

## Usage

Terminal 1:

    lein repl
    (run)
    (browser-repl)
    
Open a browser pointing to [http://localhost:3000](http://localhost:3000)    

### In IntelliJ
    
In Terminal:    

    lein repl :headless
    
Open a remote REPL, then:

    (run)
    (browser-repl)
    
Open a browser pointing to [http://localhost:3000](http://localhost:3000)
    
## Development
    
Running the specs once (SLOW):

    lein spec
    
Or autotest (SLOW to start, then FAST to develop):
    
    lein autotest
    
To see what the documentation looks like:
    
    lein marg
    
## Documentation
    
You can find the project's latest documentation in 
[Our S3 Artifact Bucket](http://s3.amazonaws.com/freefrog/docs/uberdoc.html).   

## License

Copyright © 2014 Courage Labs

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
