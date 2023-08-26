package eu.rekawek.coffeegb.memory;

import static eu.rekawek.coffeegb.cpu.BitUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.rekawek.coffeegb.AddressSpace;

public class Mmu implements AddressSpace {

    private static final Logger LOG = LoggerFactory.getLogger(Mmu.class);

    private static final AddressSpace VOID = new AddressSpace() { // 익명 클래스...
        @Override
        public boolean accepts(int address) {
            return true;
        }

        @Override
        public void setByte(int address, int value) {
            if (address < 0 || address > 0xffff) {
                throw new IllegalArgumentException("Invalid address: " + Integer.toHexString(address));
            }
            LOG.debug("Writing value {} to void address {}", Integer.toHexString(value), Integer.toHexString(address));
        }

        @Override
        public int getByte(int address) {
            if (address < 0 || address > 0xffff) {
                throw new IllegalArgumentException("Invalid address: " + Integer.toHexString(address));
            }
            LOG.debug("Reading value from void address {}", Integer.toHexString(address));
            return 0xff;
        }
    };

    private final List<AddressSpace> spaces = new ArrayList<>();
    private final Map<Integer, AddressSpace> spaceMap = new HashMap<>();

    public void addAddressSpace(AddressSpace space) {
        spaces.add(space);
    }

    @Override
    public boolean accepts(int address) {
        return true;
    }

    @Override
    public void setByte(int address, int value) {
        checkByteArgument("value", value);
        checkWordArgument("address", address);
        getSpace(address).setByte(address, value);
    }

    @Override
    public int getByte(int address) {
        checkWordArgument("address", address);
        return getSpace(address).getByte(address); // VOID ??
    }

    private AddressSpace getSpace(int address) {
        AddressSpace addressSpace = spaceMap.get(address);
        if (addressSpace != null) {
            return addressSpace;
        }
        for (AddressSpace s : spaces) {
            if (s.accepts(address)) {
                spaceMap.put(address, s);
                return s;
            }
        }
        return VOID;
    }

}
