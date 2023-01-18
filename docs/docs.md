# PRISMS 10 robot

## Integer format

### Address

```text
M M X X X X X X X Y Y Y Y Y Y Y
```

- `M`: 2 bit mark, indicating the current location's type
  - `00`: Sky island
  - `01`: Ad well
  - `10`: Mn well
  - `11`: Ex well
- `X`: 7 bit x coordinate
- `Y`: 7 bit y coordinate

Special:
- `0011 1111 1111 1111`: default location

## Shared Memory Allocation

- integer 0-7: position of every well
- integer 8-11: position of every headquarters
- integer 12-47: position of every sky island
- integer 63: memory status indicator
  - format: `I___ ____ ____ ____`
  - `I`: Whether memory is initialized, either 0 or 1.

## Robot states

### Carrier

| number | meaning                              |
|--------|--------------------------------------|
| 0      | no target, exploring the map         |
| 1      | going from headquarter to well       |
| 2      | going from headquarter to sky island |
| 3      | going back to headquarter            |

### Launcher