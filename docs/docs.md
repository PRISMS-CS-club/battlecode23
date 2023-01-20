# PRISMS 10 robot

## Integer format

### Address

```text
M M X X X X X X X Y Y Y Y Y Y Y
```

- `M`: 2 bit mark, indicating the current location's type
    - Wells
        - `01`: Ad well
        - `10`: Mn well
        - `11`: Ex well
    - Sky Islands
        - `00`: empty island
        - `01`: island occupied by our team
        - `10`: island occupied by enemy team
- `X`: 7 bit x coordinate
- `Y`: 7 bit y coordinate

Special:

- `0011 1111 1111 1111`: default location

## Shared Memory Allocation

- integer 0-7: position of every well
- integer 8-11: position of every headquarters
- integer 12-47: position of every sky island
- integer 48-51: position of every enemy headquarters
- integer 63: memory status indicator
    - format: `I___ ____ ____ ____`
    - `I`: Whether memory is initialized, either 0 or 1.

## Robot states

### Headquarter

| number                                                                | meaning                      |
|-----------------------------------------------------------------------|------------------------------|
| 0 - `initialRobots.length - 1`                                        | producing the initial robots |
| `initialRobots.length` ~ `initialRobots.length + nextAnchorRound - 1` | producing random objects     |
| `initialRobots.length + nextAnchorRound`                              | producing one anchor         |

### Carrier

| number | meaning                              |
|--------|--------------------------------------|
| 0      | no target, exploring the map         |
| 1      | going from headquarter to well       |
| 2      | going from headquarter to sky island |
| 3      | going back to headquarter            |

To prevent robots from stuck, when an robot stays in state 1, 2, or 3 for more than 450 rounds and not holding an anchor, it will self-destruct.

### Launcher

| number | meaning                                                       |
|--------|---------------------------------------------------------------|
| 0      | initial state. if cannot find any work, do random movement    |
| 1      | moving toward the destination                                 |
| 2      | staying in one fixed location without movement                |
| 3      | moving around the target with it kept in sight                |
| 4      | always do random movement. Do not actively searching for work |

Assignment of launchers

| position           | amount |
|--------------------|--------|
| around own base    | 20%    |
| at enemy's base    | 35%    |
| around sky islands | 35%    |
| random moving      | 10%    |