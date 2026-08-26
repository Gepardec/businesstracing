export interface LayoutPoint {
  x: number;
  y: number;
}

function distance(first: LayoutPoint, second: LayoutPoint): number {
  return Math.hypot(second.x - first.x, second.y - first.y);
}

function pointToward(from: LayoutPoint, to: LayoutPoint, distanceFromStart: number): LayoutPoint {
  const length = distance(from, to);
  if (length === 0) return from;
  const ratio = distanceFromStart / length;
  return { x: from.x + (to.x - from.x) * ratio, y: from.y + (to.y - from.y) * ratio };
}

function number(value: number): string {
  return String(Math.round(value * 100) / 100);
}

export function routePointAtFraction(points: readonly LayoutPoint[], fraction: number): LayoutPoint {
  if (points.length === 0) throw new Error('An edge route must contain a point.');
  if (points.length === 1) return points[0];
  const lengths = points.slice(1).map((point, index) => distance(points[index], point));
  const total = lengths.reduce((sum, length) => sum + length, 0);
  if (total === 0) return points[0];
  let remaining = total * Math.max(0, Math.min(1, fraction));
  for (let index = 0; index < lengths.length; index += 1) {
    if (remaining <= lengths[index]) return pointToward(points[index], points[index + 1], remaining);
    remaining -= lengths[index];
  }
  return points.at(-1)!;
}

export function routeLabelPosition(points: readonly LayoutPoint[], fraction: number, flowCenterX: number): LayoutPoint {
  if (points.length < 2) throw new Error('An edge route must contain at least two points.');
  const lengths = points.slice(1).map((point, index) => distance(points[index], point));
  const total = lengths.reduce((sum, length) => sum + length, 0);
  let remaining = total * Math.max(0, Math.min(1, fraction));
  let segmentIndex = lengths.length - 1;
  for (let index = 0; index < lengths.length; index += 1) {
    if (remaining <= lengths[index]) {
      segmentIndex = index;
      break;
    }
    remaining -= lengths[index];
  }
  const start = points[segmentIndex];
  const end = points[segmentIndex + 1];
  const point = pointToward(start, end, Math.min(remaining, lengths[segmentIndex]));
  if (Math.abs(end.y - start.y) >= Math.abs(end.x - start.x)) {
    return { x: point.x + (point.x >= flowCenterX ? 12 : -12), y: point.y };
  }
  return { x: point.x, y: point.y - 12 };
}

export function roundedOrthogonalPath(points: readonly LayoutPoint[], radius = 8): string {
  if (points.length < 2) throw new Error('An edge route must contain at least two points.');
  const commands = [`M ${number(points[0].x)} ${number(points[0].y)}`];
  for (let index = 1; index < points.length - 1; index += 1) {
    const previous = points[index - 1];
    const corner = points[index];
    const next = points[index + 1];
    const cornerRadius = Math.min(radius, distance(previous, corner) / 2, distance(corner, next) / 2);
    const before = pointToward(corner, previous, cornerRadius);
    const after = pointToward(corner, next, cornerRadius);
    commands.push(`L ${number(before.x)} ${number(before.y)}`);
    commands.push(`Q ${number(corner.x)} ${number(corner.y)} ${number(after.x)} ${number(after.y)}`);
  }
  const end = points.at(-1)!;
  commands.push(`L ${number(end.x)} ${number(end.y)}`);
  return commands.join(' ');
}
