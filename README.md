# netconf-watercooler

This is a simple example of a netconf server implementing a virtual "watercooler".
The implementation is based on the [lighty.io netconf simulator](https://github.com/PANTHEONtech/lighty-netconf-simulator).

## Building and Running the Watercooler

- Clone the [lighty.io netconf simulator](https://github.com/PANTHEONtech/lighty-netconf-simulator) repository.
- Clone this repository.
- Build the netconf simulator dependencies by entering the directory `ccc ` created with the first cloning
  and running Maven:
  ```
  mvn clean install -DskipTests
  ```
- Build the watercooler simulator by entering the directory `ddd` created with the second cloning
  and running Maven:
  ```
  mvn clean install -DskipTests
  ```  
- Start the watercooler device: `java -jar ...`
- Alternatively to the previous step, open the code in `dt-watercooler-device` in your IDE, inspect, modify and
  run it from there. As long as changes are done only in the code, it is sufficient to re-build this directory.
  If the YANG module in the `dt-watercooler-model/src/yang` directory is modified, the whole dt-watercooler
  project must be re-built.
- The watercooler netconf server will start listening on port `localhost:17830` (unless another port is specified
  as command line argument during startup). One simple way for interacting with it is via the
  [netconf-console2 client](https://bitbucket.org/martin_volf/ncc/src/master/):
  `netconf-console2 --host 127.0.0.1 --port 17830 -u admin -p admin`. At the prompt, you can simply type `get` or
  `get-config` to see the contents of the operational or configuration datastore. For further use, please see the
  netconf-console2 documentation.

## How it works

The watercooler is a simple application that simulates rising and falling water levels in a tank, based on its interaction
with the environment. The interaction takes places via the netconf protocol
[RFC 6241](https://datatracker.ietf.org/doc/html/rfc6241). The capabilities of netconf devices
are described by YANG modules. The YANG syntax, which offers by far more complex modeling options than used here,
is specified in [RFC 7950](https://datatracker.ietf.org/doc/html/rfc7950).

The YANG module describing the watercooler is in `dt-watercooler-model/src/main/yang`. Its tree structure
(created with [pyang](https://github.com/mbj4668/pyang)) looks as follows:

```
module: watercooler
  +--rw watercooler
     +--ro watercoolerManufacturer    DisplayString
     +--ro watercoolerModelNumber     DisplayString
     +--ro overflowIndicator?         enumeration
     +--ro fillLevel?                 uint32
     +--rw refillRate?                uint32

  rpcs:
    +---x tap
       +---w input
       |  +---w cupSize?   enumeration
       +--ro output
          +--ro remainingFillLevel?   uint32
          +--ro tapSuccesful?         enumeration

  notifications:
    +---n overflowWarning
    +---n emptyWarning
```
Besides the static `watercoolerManufacturer` and `watercoolerModelNumber` entries, the important *operational* data are
the `fillLevel`, which indicates the amount of water (in %) in the tank, and the `overflowIndicator`, which is set when
the `fillLevel` reaches 99%.

The only *configuration* data item is the `fillRate`, which can be set between 0 and 10 (% per second).

Moreover, the watercooler offers one remote procedure call (rpc) to tap an amount of water, specified by the parameter 
`cupSize` (allowed values 'S', 'M', and 'L'). Its response will indicate the remaining water in the tank and whether or
not the requested amount of water could be provided. 

There are also two *notifications* defined, to be issued by the watercooler when the water level reaches specific
thresholds. *The logic for this is not yet implemented.* Clients can *subscribe* to such notifications and will be
henceforth informed automatically about such events.

The Java source code is in the `dt-watercooler-device/src/main/java` directory. The `Main` class builds the Netconf device by making 
use of the lighty.io implementation. It also starts the Watercooler 'operation'. The functionality is organized in the
following classes:
- `DOMDataService`: Provides methods to read from and write to the operational and configuration datastore. 
- `WatercoolerServiceTapProcessor`: A 'processor' for the `tap`rpc that is provided to the underlying Netconf device
  implementation provided by the lighty.io Netconf device. The device will invoke the 'processor' when it receives
  a `tap` rpc.
- `WatercoolerServiceImpl`: This class provides the logic behind the `tap` rpc.
- `WatercoolerDeviceRunner`: The runner runs periodically in its own thread to adjust the water level based on the
   refill rate. It also turns the overflow indicator on or off.

In the  `dt-watercooler-device/src/main/resources` directory, there are two `xml` files containing the data that will
be written at startup to the watercooler's operational and configuration data store. The third file
(`log4j.properties`) is a 1:1 copy of the `log4j.properties` in a parent project and as such could be removed without
any effect. It is useful for quickly changing the logging to `debug` or `trace` level during own experiments with the
watercooler, if desired.

