package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.Game;
import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.PlayerScore;
import com.loxon.javachallenge.memory.api.communication.commands.CommandAllocate;
import com.loxon.javachallenge.memory.api.communication.commands.CommandFortify;
import com.loxon.javachallenge.memory.api.communication.commands.CommandFree;
import com.loxon.javachallenge.memory.api.communication.commands.CommandRecover;
import com.loxon.javachallenge.memory.api.communication.commands.CommandScan;
import com.loxon.javachallenge.memory.api.communication.commands.CommandStats;
import com.loxon.javachallenge.memory.api.communication.commands.CommandSwap;
import com.loxon.javachallenge.memory.api.communication.commands.ResponseScan;
import com.loxon.javachallenge.memory.api.communication.commands.ResponseStats;
import com.loxon.javachallenge.memory.api.communication.commands.ResponseSuccessList;
import com.loxon.javachallenge.memory.api.communication.general.Command;
import com.loxon.javachallenge.memory.api.communication.general.Response;
import org.junit.*;
import org.junit.rules.TestName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MemoryRules {

    private final static MemoryState F  = MemoryState.FREE;
    private final static MemoryState S  = MemoryState.SYSTEM;
    private final static MemoryState C  = MemoryState.CORRUPT;
    private final static MemoryState AX = MemoryState.ALLOCATED;
    private final static MemoryState AM = MemoryState.OWNED_ALLOCATED;
    private final static MemoryState FX = MemoryState.FORTIFIED;
    private final static MemoryState FM = MemoryState.OWNED_FORTIFIED;

    private final static int      GAME_ROUNDS = 10;
    @Rule
    public               TestName name        = new TestName();
    private              Game     game        = null;
    private              Player   pA          = null;
    private              Player   pB          = null;

    private static void assertResponse( final List<Response> actual, final Response... expected ) {
        final Map<Player, Response> actualMap = actual.stream().collect(Collectors.toMap(Response::getPlayer, Function.identity()));
        for ( Response r : expected ) {
            final Response rsp = actualMap.get(r.getPlayer());
            Assert.assertNotNull("Missing response.", rsp);
            Assert.assertEquals(r, rsp);
        }
    }

    private static Command allocate( final Player player, final Integer cell0, final Integer cell1 ) {
        return new CommandAllocate(player, asList(cell0, cell1));
    }

    private static Command free( final Player player, final Integer cell0, final Integer cell1 ) {
        return new CommandFree(player, asList(cell0, cell1));
    }

    private static Command recover( final Player player, final Integer cell0, final Integer cell1 ) {
        return new CommandRecover(player, asList(cell0, cell1));
    }

    private static Command fortify( final Player player, final Integer cell0, final Integer cell1 ) {
        return new CommandFortify(player, asList(cell0, cell1));
    }

    private static Command swap( final Player player, final Integer cell0, final Integer cell1 ) {
        return new CommandSwap(player, asList(cell0, cell1));
    }

    private static CommandScan scan( final Player player, final int cell ) {
        return new CommandScan(player, cell);
    }

    private static ResponseSuccessList list( final Player player, final Integer cell0, final Integer cell1 ) {
        return new ResponseSuccessList(player,
            cell0 == null && cell1 == null ? Collections.emptyList() :
                cell0 != null && cell1 == null ? Collections.singletonList(cell0) :
                    Arrays.asList(cell0, cell1)
        );
    }

    private static ResponseScan cells( final Player player, final int cell, final MemoryState s1, final MemoryState s2, final MemoryState s3,
        final MemoryState s4 ) {
        return new ResponseScan(player, cell, Arrays.asList(s1, s2, s3, s4));
    }

    private static List<Integer> asList( final Integer cell0, final Integer cell1 ) {
        return Arrays.asList(cell0, cell1);
    }

    protected Game createGame() {
        return GameImplementationFactory.get();
    }

    @Before
    public void beforeEachTest() {
        game = createGame();
        Assert.assertNotNull("Game implementation is required.", game);
        pA = game.registerPlayer("a");
        pB = game.registerPlayer("b");
        game.startGame(Arrays.asList(
            F, F, F, F, F, S, S, S,
            F, F, F, F, F, F, F, F,
            F, F, F, F, F, F, F, F), GAME_ROUNDS);
    }

    @Test
    public void testScan() {
        assertResponse( // check system cells
            game.nextRound(
                scan(pA, 4)),
            cells(pA, 4, F, S, S, S));
        assertResponse( // check system cells - index is inside the same block
            game.nextRound(
                scan(pA, 6)),
            cells(pA, 4, F, S, S, S));
        assertResponse( // at the end
            game.nextRound(
                scan(pA, 23)),
            cells(pA, 20, F, F, F, F));
    }

    @Test
    public void testScanInvalid() {
        assertResponse( // outside of allowed area
            game.nextRound(
                scan(pA, 24)),
            new ResponseScan(pA, -1, Collections.emptyList()));
        assertResponse( // outside of allowed area
            game.nextRound(
                scan(pA, -1)),
            new ResponseScan(pA, -1, Collections.emptyList()));
    }

    @Test
    public void testAllocate() {

        assertResponse( // allocating free cells, no collisions between players
            game.nextRound(
                allocate(pA, 0, 1),
                allocate(pB, 2, 3)),
            list(pA, 0, 1),
            list(pB, 2, 3));

        assertResponse( // allocating free cells, collisions will fail
            game.nextRound(
                allocate(pA, 8, 9),
                allocate(pB, 9, 10)),
            list(pA, 8, null),
            list(pB, 10, null));

        assertResponse( // collisions will be corrupt, scan response index will be the first cell
            game.nextRound(
                scan(pA, 8),
                scan(pB, 10)),
            cells(pA, 8, AM, C, AX, F),
            cells(pB, 8, AX, C, AM, F));

        assertResponse( // system cells cannot be changed
            game.nextRound(
                allocate(pB, 5, 6)),
            list(pB, null, null));
    }

    @Test
    public void testAllocateInvalid() {

        assertResponse( // invalid indexes, outside of accessible memory
            game.nextRound(
                allocate(pA, -100, +100)),
            list(pA, null, null));

        assertResponse( // only one invalid index outside of accessible memory - no allocation at all
            game.nextRound(
                allocate(pA, 0, +100)),
            list(pA, null, null));

        assertResponse( // invalid indexes, not in the same block
            game.nextRound(
                allocate(pA, 2, 10)),
            list(pA, null, null));

        assertResponse( // more cells then allowed
            game.nextRound(
                new CommandAllocate(pA, Arrays.asList(0, 1, 2))),
            list(pA, null, null));

    }

    @Test
    public void testAllocateCorrupt() {

        assertResponse( // corrupting cell
            game.nextRound(
                allocate(pA, 2, 2)),
            list(pA, null, null));

        assertResponse( // corrupt cell allocation is not succeeded
            game.nextRound(
                allocate(pA, 2, null)),
            list(pA, null, null));

        assertResponse( // cell will be corrupt
            game.nextRound(
                scan(pA, 2)),
            cells(pA, 0, F, F, C, F));
    }

    @Test
    public void testFree() {

        game.nextRound( // create allocated cells
            allocate(pA, 0, 1),
            allocate(pB, 2, 3));

        assertResponse( // cells are occupied
            game.nextRound(
                scan(pA, 0)),
            cells(pA, 0, AM, AM, AX, AX));

        game.nextRound( // corrupting cell
            allocate(pA, 2, 2));

        assertResponse( // free cells of each other
            game.nextRound(
                free(pB, 0, 1),
                free(pA, 2, 3)),
            list(pB, 0, 1),
            list(pA, 2, 3));

        assertResponse( // all cells should be free, even corrupt ones
            game.nextRound(
                scan(pA, 0)),
            cells(pA, 0, F, F, F, F));

        game.nextRound( // create allocated cells
            allocate(pA, 0, 1),
            allocate(pB, 2, 3));

        assertResponse( // free own cells with collision
            game.nextRound(
                free(pA, 1, 0),
                free(pB, 1, 3)),
            list(pA, 0, null),
            list(pB, 3, null));

        assertResponse( // system cell cannot be freed, freeing free cell is allowed
            game.nextRound(
                free(pB, 4, 5)),
            list(pB, 4, null));
    }

    @Test
    public void testFreeInvalid() {

        assertResponse( // invalid indexes, outside of accessible memory
            game.nextRound(
                free(pA, -100, +100)),
            list(pA, null, null));

        assertResponse( // more cells then allowed
            game.nextRound(
                new CommandFree(pA, Arrays.asList(1, 2, 3))),
            list(pA, null, null));
    }

    @Test
    public void testRecover() {

        game.nextRound( // create a corrupted, a free and an allocated cell
            allocate(pA, 0, 0),
            allocate(pB, 2, null));

        assertResponse( // recover the corrupted and the free ones, works only for corrupted
            game.nextRound(
                recover(pA, 0, 1)),
            list(pA, 0, null));

        assertResponse( // allocated one will be corrupt as well
            game.nextRound(
                recover(pA, 2, null)),
            list(pA, null, null));

        assertResponse( // corrupted one is allocated, but others are corrupted
            game.nextRound(
                scan(pA, 0)),
            cells(pA, 0, AM, C, C, F));

        assertResponse( // system blocks are untouchable
            game.nextRound(
                recover(pB, 5, 6)),
            list(pB, null, null));

        assertResponse( // multiple access creates corrupt cells
            game.nextRound(
                recover(pA, 0, 3),
                recover(pB, 3, 0)));

        assertResponse( // all corruptified
            game.nextRound(
                scan(pA, 0)),
            cells(pA, 0, C, C, C, C));
    }

    @Test
    public void testFortify() {

        game.nextRound( //Just allocate some cells
            allocate(pA, 12, 15),
            allocate(pB, 16, 19));

        game.nextRound( // multiple access creates corrupt cells
            fortify(pB, 18, null),
            allocate(pA, 18, null));

        assertResponse( // fortify allocated cells  (for the other player), free and corrupt cells are not allowed
            game.nextRound(
                fortify(pB, 12, 14),
                fortify(pA, 16, 18)),
            list(pB, 12, null),
            list(pA, 16, null));

        assertResponse( // trying to corrupt fortified cells won't work
            game.nextRound(
                fortify(pB, 12, 16),
                fortify(pA, 16, 12)),
            list(pB, null, null),
            list(pA, null, null));

        assertResponse( // cells become fortified
            game.nextRound(
                scan(pA, 12),
                scan(pB, 16)),
            cells(pA, 12, FM, F, F, AM),
            cells(pB, 16, FM, F, C, AM));

        assertResponse( // cells become fortified - reversed owners
            game.nextRound(
                scan(pB, 12),
                scan(pA, 16)),
            cells(pB, 12, FX, F, F, AX),
            cells(pA, 16, FX, F, C, AX));

        assertResponse( // freeing (or any other operation) will fail
            game.nextRound(
                free(pB, 12, null),
                free(pA, 16, null)),
            list(pB, null, null),
            list(pA, null, null));
    }

    @Test
    public void testSwap() {

        game.nextRound( // allocate cells
            allocate(pA, 0, 1),
            allocate(pB, 2, 3));

        assertResponse( // swap cells (not owned)
            game.nextRound(
                swap(pB, 1, 9),
                swap(pA, 2, 10)),
            list(pA, 2, 10),
            list(pB, 1, 9));

        assertResponse( // check new place
            game.nextRound(
                scan(pA, 0),
                scan(pB, 8)),
            cells(pA, 0, AM, F, F, AX),
            cells(pB, 8, F, AX, AM, F));

        assertResponse( // swap cells (owned) with parallel access, all get corrupt
            game.nextRound(
                swap(pB, 3, 1),
                swap(pA, 0, 3)),
            list(pA, null, null),
            list(pB, null, null));

        assertResponse( // check new state
            game.nextRound(
                scan(pA, 0)),
            cells(pA, 0, C, C, F, C));

        assertResponse( // swap only cell results no effect, same cells get corrupt
            game.nextRound(
                swap(pB, null, 16),
                swap(pA, 19, 19)),
            list(pA, null, null),
            list(pB, null, null));

        assertResponse( // check new state
            game.nextRound(
                scan(pA, 16)),
            cells(pA, 16, F, F, F, C));

        game.nextRound( // allocate cells
            allocate(pA, 12, 15));

        final Player pC = game.registerPlayer("c");
        assertResponse( // swap in a chain with 3 player
            game.nextRound(
                swap(pB, 15, 13),
                swap(pA, 9, 10),
                swap(pC, 15, 9)),
            list(pA, null, null),
            list(pB, null, null),
            list(pC, null, null));

        assertResponse( // check new state
            game.nextRound(
                scan(pA, 8),
                scan(pB, 12)),
            cells(pA, 8, F, C, C, F),
            cells(pB, 12, AX, C, F, C));
    }

    @Test
    public void testSwapInvalid() {

        assertResponse( // swap cells outside of memory range
            game.nextRound(
                swap(pA, 1, +100)),
            list(pA, null, null));

        assertResponse( // swap cells outside of memory range
            game.nextRound(
                swap(pA, +100, 1)),
            list(pA, null, null));

        assertResponse( // swap 3 cell
            game.nextRound(
                new CommandSwap(pA, Arrays.asList(1, 2, 3))),
            list(pA, null, null));

        assertResponse( // swap 1 cells
            game.nextRound(
                new CommandSwap(pA, Collections.singletonList(1))),
            list(pA, null, null));

        assertResponse( // send null values
            game.nextRound(
                new CommandSwap(pA, Arrays.asList(null, null))),
            list(pA, null, null));
    }

    @Test
    public void testStatsAndScores() {

        final ResponseStats startOfGame = (ResponseStats) game.nextRound(new CommandStats(pA)).get(0);
        Assert.assertEquals(24, startOfGame.getCellCount());
        Assert.assertEquals(3, startOfGame.getSystemCells());

        Assert.assertEquals(GAME_ROUNDS - 1, startOfGame.getRemainingRounds());
        Assert.assertEquals(0, startOfGame.getOwnedCells());
        Assert.assertEquals(0, startOfGame.getAllocatedCells());
        Assert.assertEquals(0, startOfGame.getCorruptCells());

        game.nextRound( // allocate cells
            allocate(pA, 12, 13),
            allocate(pB, 16, 17));

        game.nextRound( // allocate more, corrupt a bit
            allocate(pA, 14, 15),
            allocate(pB, 18, 18));

        game.nextRound( // fortify
            fortify(pA, 12, 13),
            fortify(pB, 16, 17));

        assertResponse( // check what we wanted
            game.nextRound(
                scan(pA, 12),
                scan(pB, 16)),
            cells(pA, 12, FM, FM, AM, AM),
            cells(pB, 16, FM, FM, C, F));

        final ResponseStats endOfGame = (ResponseStats) game.nextRound(new CommandStats(pA)).get(0);
        Assert.assertEquals(24, endOfGame.getCellCount());
        Assert.assertEquals(3, endOfGame.getSystemCells());

        Assert.assertEquals(4, endOfGame.getRemainingRounds());
        Assert.assertEquals(14, endOfGame.getFreeCells());
        Assert.assertEquals(4, endOfGame.getOwnedCells());
        Assert.assertEquals(2, endOfGame.getAllocatedCells());
        Assert.assertEquals(1, endOfGame.getCorruptCells());

        final Map<Player, PlayerScore> scores = game.getScores().stream()
            .collect(Collectors.toMap(PlayerScore::getPlayer, Function.identity()));

        PlayerScore pAScore = scores.get(pA);
        PlayerScore pBScore = scores.get(pB);

        Assert.assertEquals(4, pAScore.getOwnedCells());
        Assert.assertEquals(1, pAScore.getOwnedBlocks());
        Assert.assertEquals(2, pAScore.getFortifiedCells());
        Assert.assertEquals(8, pAScore.getTotalScore());

        Assert.assertEquals(2, pBScore.getOwnedCells());
        Assert.assertEquals(0, pBScore.getOwnedBlocks());
        Assert.assertEquals(2, pBScore.getFortifiedCells());
        Assert.assertEquals(2, pBScore.getTotalScore());
    }

    @Test
    public void testHack() {

        //Unregistered player does not get response
        final Player p2 = new Player("hacker");
        Assert.assertEquals(0, game.nextRound(allocate(p2, 0, 1)).size());

        //Only first request is handled from each player
        assertResponse(
            game.nextRound(
                allocate(pA, 0, 1),
                allocate(pA, 16, 17),
                allocate(pB, 2, 3)),
            list(pA, 0, 1),
            list(pB, 2, 3));

        assertResponse(
            game.nextRound(
                scan(pB, 16)),
            cells(pB, 16, F, F, F, F));
    }

    @After
    public void afterEachTest() {
        if ( game != null ) {
            System.out.println(game.visualize());
            final List<Response> statsResponse = game.nextRound(new CommandStats(pA));
            if ( statsResponse != null && !statsResponse.isEmpty() ) {
                System.out.println(statsResponse.get(0));
            }
            System.out.println();
            game.getScores().forEach(s -> System.out.printf("%s - %s\r\n", s.getPlayer().getName(), s.toString()));
        }
    }
}
