# freefrog

Exquisite Organization -- For Free

## Information

This project is being integrated using
[Travis-CI](https://travis-ci.org/).

[![Build Status](https://travis-ci.org/couragelabs/freefrog.svg?branch=master)](https://travis-ci.org/couragelabs/freefrog)

[![Dependencies Status](http://jarkeeper.com/couragelabs/freefrog/status.svg)](http://jarkeeper.com/couragelabs/freefrog)

Artifacts generated:

 * [API Documentation](http://s3.amazonaws.com/freefrog/docs/uberdoc.html)
 * [Specs](http://s3.amazonaws.com/freefrog/docs/specs.txt)
 * [Code Cleanliness Reports]
   (http://s3.amazonaws.com/freefrog/docs/kibit.txt) (empty is good)

## Development

Running the specs once (SLOW):

    lein spec

Or autotest (SLOW to start, then FAST to develop):

    lein autotest

To see what the documentation looks like:

    lein marg && open docs/uberdoc.html

## Running it locally

    lein freefrog

## Operating using Docker

Docker is our preferred approach for operating Freefrog. An example file
can be found in the root:

    ./run-with-docker

## Getting Help

If the information above isn't telling you what you need to know, come chat
with us! We hang out in
[our IRC Channel](https://kiwiirc.com/client/irc.freenode.net/?nick=guest|?#couragelabs)
pretty regularly.

## License

Copyright © 2015 Courage Labs

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
