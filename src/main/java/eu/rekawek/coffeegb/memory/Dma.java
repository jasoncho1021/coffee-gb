package eu.rekawek.coffeegb.memory;

import eu.rekawek.coffeegb.AddressSpace;
import eu.rekawek.coffeegb.cpu.SpeedMode;

public class Dma implements AddressSpace {

    private final AddressSpace addressSpace;

    private final AddressSpace oam;

    private final SpeedMode speedMode;

    private boolean transferInProgress;

    private boolean restarted;

    private int from;

    private int ticks;

    public Dma(AddressSpace addressSpace, AddressSpace oam, SpeedMode speedMode) { // Mmu, fe00
        this.addressSpace = addressSpace;
        this.speedMode = speedMode;
        this.oam = oam;
    }

    @Override
    public boolean accepts(int address) {
        return address == 0xff46;
    }

    /* 648 cycle = 4*162cycle,,, 4?
     * dma 가 emul 시작 단계에서 sprite oam으로 복사할때 불리면 
        근데, 에뮬키면 gpu tick은 vblank가 아니라 맨 처음부터 oamsearch가는데,,,vblank..lcdoff인지 어케체크하지 처음부터 oamsearch돌리면?
     * -> 위 문장은 dma.tick()과 oamsearch를 혼용해서 오해한 것이다.
     * oamscan단계는 말그대로 dma로 옮겨진 결과를 읽는 거다. dma는 그 전에 ram으로 옮겨놓는 거고
     * 
     * 근데 저 아래 648 tick이 지나야 한다는 건 도대체 ... 첫줄 scan(oam,vram)과 hblank 456 + 2번째줄 80(oam) + 172(accessingVram) 단계에 한다고? 
     * 
     * 4*162
     * 4cycle * 160byte?? ---> cgb라서 그런가?
     * 복사하는 instruction 하나에 4cycle?   
     * 
     * dec 1 cycle, jr 3cycle, 4cycle*40 이라는데??
     * 
     * GPU.tick() { phase.tick(); }
     * 80 oamSearch tick,
     * 160 pixelTransfer tick
     * 456-240=216 hblank(tickInLine),, 456에 맞춰줌
     * vblank말고 두번째 줄 oamsearch로 넘어감
     * 648-456 = 192
     * pixeltransfer phase에서 dma 하네
     */
    /**
     * 0x00 - 0xa0 --> 160
     * 160 * 4cycle(복사) = 640
     * 2 * 4cycle ,, extra cycles for what?
     * 648cycle
     */
    public void tick() {
        if (transferInProgress) { // 6*108, 8*81(scanline 80clocks, accessing oam),,, vblank(++tick < 456),,and lcd off
            if (++ticks >= 648 / speedMode.getSpeedMode()) { // tick이 gpu tick한번 할 때마다 하나씩 오르는데 648의미가 ?? vblank hblank 사이에 걸칠 것 같은데, 그리고 ++tick넘자 마자 바로 아래 for문으로 한 번으로 전송
                transferInProgress = false;
                restarted = false;
                ticks = 0;
                for (int i = 0; i < 0xa0; i++) { // oam = Ram(0xfe00, 0x00a0); 1010 0000,, --> 160byte 40개*4byte,, cycle은 ld, inc, ld, 1byte 옮기는데 4cycle?
                    oam.setByte(0xfe00 + i, addressSpace.getByte(from + i));
                }
            }
        }
    }

    @Override
    public void setByte(int address, int value) { // ROM에서 0xff46에 값 쓰는 코드 --> DMA시작!
    	//System.out.println("DMA start");
        from = value * 0x100;
        restarted = isOamBlocked();
        ticks = 0;
        transferInProgress = true;
    }

    @Override
    public int getByte(int address) {
        return 0;
    }

    public boolean isOamBlocked() {
        return restarted || (transferInProgress && ticks >= 5);
    }
}